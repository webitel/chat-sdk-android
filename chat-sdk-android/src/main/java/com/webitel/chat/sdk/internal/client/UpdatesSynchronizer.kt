package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.transport.dto.ThreadUpdatesDto
import com.webitel.chat.sdk.internal.transport.dto.UpdatesResponseDto


internal interface UpdatesSynchronizerDelegate {

    fun fetchUpdates(cursor: String, onComplete: (Result<UpdatesResponseDto>) -> Unit)

    /** Applies one page of recovered changes. Called on the sync thread. */
    fun applyUpdates(threads: List<ThreadUpdatesDto>)

    /** Incremental sync is impossible, all client state must be reloaded. */
    fun resyncRequired()
}


/**
 * Owns the updates cursor and recovers changes missed
 * while the realtime connection was unavailable.
 *
 * Realtime events are routed through [submit]: while a sync is running
 * they are buffered and replayed afterwards, skipping those already
 * covered by the recovered changes.
 *
 * All state is confined to the `chat-sdk-sync` thread.
 */
internal class UpdatesSynchronizer(
    private val execution: ExecutionContext,
    maxAttempts: Int,
    private val retryDelayMs: (attempt: Int) -> Long
) {

    private class PendingEvent(
        val cursor: String?,
        val work: () -> Unit
    )

    var delegate: UpdatesSynchronizerDelegate? = null

    private val maxAttempts = maxAttempts.coerceAtLeast(1)

    private var cursor: String? = null

    /**
     * False between [reset] and [activate]: late frames of an ended
     * session are dropped, so they neither reach listeners nor seed
     * the cursor of the next one.
     */
    private var isActive = true
    private var isSyncing = false
    private val pending = mutableListOf<PendingEvent>()
    private var generation = 0

    private companion object {
        const val TAG = "UpdatesSynchronizer"

        /** Upper bound of pages per sync, protects against an endless `has_more`. */
        const val MAX_PAGES = 50

        /**
         * Cursors are monotonic numeric strings. Non-numeric cursors
         * cannot be ordered and are treated as newer when they differ.
         */
        fun isNewer(candidate: String, current: String): Boolean {
            val lhs = candidate.toLongOrNull()
            val rhs = current.toLongOrNull()

            if (lhs != null && rhs != null) {
                return lhs > rhs
            }

            return candidate != current
        }
    }


    /**
     * Server confirmed the connection (`connected_event`).
     *
     * Without a known cursor this is the first connection in the session:
     * the server cursor becomes the baseline. Otherwise it is a reconnection
     * and changes missed since the known cursor are recovered, unless the
     * server cursor is not newer than the known one (nothing was missed).
     */
    fun onConnected(serverCursor: String?) {
        execution.sync {
            if (!isActive) return@sync

            val from = cursor

            if (from == null) {
                cursor = serverCursor
                return@sync
            }

            if (serverCursor != null && !isNewer(serverCursor, from)) {
                logger.debug(TAG, "cursor $from is up to date, sync skipped")
                return@sync
            }

            startSync(from)
        }
    }


    /**
     * Processes a realtime event and advances the cursor, or buffers it
     * while a sync is running.
     *
     * Buffering is required because the updates response is a snapshot
     * taken before it reaches the client: applying live events first would
     * let that older snapshot overwrite newer state (e.g. revive a message
     * deleted in the meantime). Keeping the cursor frozen until the sync
     * succeeds also prevents skipping over an unrecovered gap.
     */
    fun submit(eventCursor: String?, work: () -> Unit) {
        execution.sync {
            if (!isActive) return@sync

            if (isSyncing) {
                pending += PendingEvent(eventCursor, work)
                return@sync
            }

            perform(work)
            advance(eventCursor)
        }
    }


    /** Runs an ephemeral event (e.g. typing): never buffered, dropped between [reset] and [activate]. */
    fun submitEphemeral(work: () -> Unit) {
        execution.sync {
            if (isActive) perform(work)
        }
    }


    /** Resumes cursor tracking after [reset] (e.g. on `connect()`). */
    fun activate() {
        execution.sync {
            isActive = true
        }
    }


    /**
     * Forgets the cursor and all pending state (e.g. on session end).
     * Cursor tracking stays suspended until [activate].
     */
    fun reset() {
        execution.sync {
            isActive = false
            generation++
            cursor = null
            isSyncing = false
            pending.clear()
        }
    }


    private fun startSync(from: String) {
        generation++
        isSyncing = true

        logger.debug(TAG, "sync started from cursor $from")

        fetchPage(from, generation, attempt = 0, page = 0)
    }


    /**
     * Called on the sync thread. A changed [generation] cancels the sync.
     *
     * Every path must end the sync (or schedule the next step):
     * a sync left running would buffer realtime events forever.
     */
    private fun fetchPage(from: String, generation: Int, attempt: Int, page: Int) {
        val delegate = delegate

        if (delegate == null) {
            // Nobody to apply changes to: release buffered events
            // instead of keeping the sync running forever
            flushPending()
            finishSync()
            return
        }

        try {
            delegate.fetchUpdates(from) { result ->
                execution.sync {
                    onPageResult(result, from, generation, attempt, page)
                }
            }
        } catch (t: Throwable) {
            logger.error(TAG, "sync request failed: $t")
            giveUp()
        }
    }


    private fun onPageResult(
        result: Result<UpdatesResponseDto>,
        from: String,
        generation: Int,
        attempt: Int,
        page: Int
    ) {
        if (this.generation != generation) return

        result.fold(
            onSuccess = { response ->
                val next = try {
                    applyPage(response, from, page)
                } catch (t: Throwable) {
                    logger.error(TAG, "sync page processing failed: $t")
                    giveUp()
                    return
                }

                if (next != null) {
                    fetchPage(next, generation, attempt = 0, page = page + 1)
                }
            },
            onFailure = { error ->
                val failed = attempt + 1
                logger.warn(TAG, "sync attempt $failed failed: $error")

                if (failed >= maxAttempts) {
                    giveUp()
                    return
                }

                execution.syncDelayed(retryDelayMs(failed)) {
                    if (this.generation != generation) return@syncDelayed
                    fetchPage(from, generation, failed, page)
                }
            }
        )
    }


    /**
     * Applies a page. Returns the cursor for the next page,
     * or null when the sync is finished.
     */
    private fun applyPage(response: UpdatesResponseDto, from: String, page: Int): String? {
        if (response.resync) {
            logger.debug(TAG, "server requested full resync")

            // Buffered events newer than the server cursor are still valid
            cursor = response.cursor
            flushPending()
            finishSync()
            delegate?.resyncRequired()
            return null
        }

        delegate?.applyUpdates(response.threads)
        cursor = response.cursor

        if (response.hasMore) {
            if (!isNewer(response.cursor, from)) {
                // Requesting the same cursor again would loop forever
                logger.warn(TAG, "has_more with non-advancing cursor ${response.cursor}, sync stopped")
            } else if (page + 1 >= MAX_PAGES) {
                // Too many changes to recover incrementally
                logger.warn(TAG, "sync exceeded $MAX_PAGES pages")
                giveUp()
                return null
            } else {
                return response.cursor
            }
        }

        flushPending()
        finishSync()
        return null
    }


    /** Replays buffered events not covered by the recovered changes. */
    private fun flushPending() {
        val events = pending.toList()
        pending.clear()

        for (event in events) {
            val eventCursor = event.cursor
            val current = cursor

            if (eventCursor != null && current != null && !isNewer(eventCursor, current)) {
                continue
            }

            perform(event.work)
            advance(eventCursor)
        }
    }


    /** Sync kept failing: the gap cannot be recovered incrementally. */
    private fun giveUp() {
        logger.error(TAG, "sync failed, full resync required")

        val events = pending.toList()
        pending.clear()
        finishSync()

        events.forEach {
            perform(it.work)
            advance(it.cursor)
        }

        delegate?.resyncRequired()
    }


    private fun finishSync() {
        isSyncing = false
        logger.debug(TAG, "sync finished at cursor ${cursor ?: "-"}")
    }


    private fun advance(eventCursor: String?) {
        eventCursor ?: return

        val current = cursor
        if (current != null && !isNewer(eventCursor, current)) {
            return
        }

        cursor = eventCursor
    }


    /** A failing event must not stop processing of the following ones. */
    private fun perform(work: () -> Unit) {
        try {
            work()
        } catch (t: Throwable) {
            logger.error(TAG, "event processing failed: $t")
        }
    }
}
