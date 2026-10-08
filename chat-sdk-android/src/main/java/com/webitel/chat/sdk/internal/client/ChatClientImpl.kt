package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatClient
import com.webitel.chat.sdk.ChatClientListener
import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.ChatEventListener
import com.webitel.chat.sdk.ConnectionListener
import com.webitel.chat.sdk.ConnectionState
import com.webitel.chat.sdk.Contact
import com.webitel.chat.sdk.ContactId
import com.webitel.chat.sdk.ContactRequest
import com.webitel.chat.sdk.Dialog
import com.webitel.chat.sdk.DialogEvent
import com.webitel.chat.sdk.DialogRequest
import com.webitel.chat.sdk.DialogSyncChanges
import com.webitel.chat.sdk.DownloadListener
import com.webitel.chat.sdk.DownloadRequest
import com.webitel.chat.sdk.EditMessageResult
import com.webitel.chat.sdk.ForwardMessagesResult
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageAction
import com.webitel.chat.sdk.MessageDeletion
import com.webitel.chat.sdk.MessageDeletionResult
import com.webitel.chat.sdk.MessageEvent
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageSearchRequest
import com.webitel.chat.sdk.MessageSearchSlice
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.Page
import com.webitel.chat.sdk.ActivityEvent
import com.webitel.chat.sdk.ReactionResult
import com.webitel.chat.sdk.ReceiptEvent
import com.webitel.chat.sdk.TypingRequest
import com.webitel.chat.sdk.UploadListener
import com.webitel.chat.sdk.UploadRequest
import com.webitel.chat.sdk.internal.api.ChatApiDelegate
import com.webitel.chat.sdk.internal.api.FileUploader
import com.webitel.chat.sdk.internal.api.HttpFileDownloader
import com.webitel.chat.sdk.internal.api.TransferTaskImpl
import com.webitel.chat.sdk.internal.auth.AuthManager
import com.webitel.chat.sdk.internal.extensions.toChatError
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDeletedEventDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionEventDto
import com.webitel.chat.sdk.internal.transport.dto.MessageStatusEventDto
import com.webitel.chat.sdk.internal.transport.dto.ReadPosition
import com.webitel.chat.sdk.internal.transport.dto.ReceiptKind
import com.webitel.chat.sdk.internal.transport.dto.ThreadUpdatesDto
import com.webitel.chat.sdk.internal.transport.dto.TypingDto
import com.webitel.chat.sdk.internal.transport.dto.UpdatesResponseDto
import com.webitel.chat.sdk.internal.transport.realtime.RealtimeListener
import com.webitel.chat.sdk.internal.transport.realtime.RealtimeTransport
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.math.pow

internal class ChatClientImpl(
    private val clientContext: ClientContext,
    private val api: ChatApiDelegate,
    private val authManager: AuthManager,
    private val realtime: RealtimeTransport,
    private val fileUploader: FileUploader,
    private val fileDownloader: HttpFileDownloader,
    private val hub: RealtimeHub,
    private val execution: ExecutionContext): ChatClient, UpdatesSynchronizerDelegate {
    private var retryAttempt = 0
    private var realtimeEnabled = false
    private var backoffTask: TimerTask? = null

    private val dialogFactory = DialogFactory(
        client = this,
        realtimeHub = hub
    )

    // Initial request plus one retry
    private val synchronizer = UpdatesSynchronizer(
        execution = execution,
        maxAttempts = 2,
        retryDelayMs = { 1000 }
    )

    override val connectionState: ConnectionState
        get() {
            // 1. If a backoff timer is active, we are currently in the reconnection process
            if (backoffTask != null) return ConnectionState.Connecting

            // 2. If no backoff is active, return the actual state from the realtime provider
            return realtime.connectionState
        }

    val currentUserId: String?
        get() {
            return authManager.currentContact?.id
        }


    companion object {
        val TAG = "ChatClientImpl"
        val logger: WLogger = WLogger()
    }


    init {
        logger.level = clientContext.logLevel
        realtime.setListener(realtimeListener())
        synchronizer.delegate = this
    }


    override fun sendMessage(
        target: MessageTarget,
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable {
        return callCancellableWithAuthRetry(
            call = { callback ->
                api.sendMessage(target, options, callback)
            },
            onComplete = onComplete
        )
    }


    override fun sendAction(
        messageId: String,
        action: MessageAction,
        onComplete: (Result<Unit>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.sendAction(messageId, action, callback)
            },
            onComplete = onComplete
        )
    }


    fun sendTyping(
        dialogId: String,
        request: TypingRequest,
        onComplete: (Result<Unit>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.sendTyping(dialogId, request, callback)
            },
            onComplete = onComplete
        )
    }


    fun markAsRead(
        dialogId: String,
        position: ReadPosition,
        onComplete: (Result<Unit>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.markAsRead(dialogId, position, callback)
            },
            onComplete = onComplete
        )
    }


    override fun setReaction(
        messageId: String,
        emoji: String,
        sendId: String?,
        onComplete: (Result<ReactionResult>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.setReaction(messageId, emoji, sendId, callback)
            },
            onComplete = onComplete
        )
    }


    override fun deleteMessages(
        ids: List<String>,
        onComplete: (Result<MessageDeletionResult>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.deleteMessages(ids, callback)
            },
            onComplete = onComplete
        )
    }


    override fun forwardMessages(
        ids: List<String>,
        target: MessageTarget,
        sendId: String,
        onComplete: (Result<ForwardMessagesResult>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.forwardMessages(ids, target, sendId, callback)
            },
            onComplete = onComplete
        )
    }


    override fun editMessage(
        messageId: String,
        text: String,
        onComplete: (Result<EditMessageResult>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.editMessage(messageId, text, callback)
            },
            onComplete = onComplete
        )
    }


    override fun getDialogs(request: DialogRequest, onComplete: (Result<Page<Dialog>>) -> Unit) {
        callWithAuthRetry(
            call = { callback ->
                api.getDialogs(request) { result ->
                    callback(
                        result.map { page ->
                            Page(
                                page.page,
                                page.items.map { dialogFactory.getOrCreate(it) },
                                page.hasNext
                            )
                        }
                    )
                }
            },
            onComplete = onComplete
        )
    }


    override fun getOrCreateDialog(
        contactId: ContactId,
        onComplete: (Result<Dialog>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.getOrCreateDialog(contactId) { result ->
                    callback(
                        result.map { dto ->
                            dialogFactory.getOrCreate(dto)
                        }
                    )
                }
            },
            onComplete = onComplete
        )
    }


    override fun getContacts(request: ContactRequest, onComplete: (Result<Page<Contact>>) -> Unit) {
        callWithAuthRetry(
            call = { callback ->
                api.getContacts(request) { result ->
                    callback(
                        result.map { page ->
                            Page(
                                page.page,
                                page.items.map { it.toDomain() },
                                page.hasNext
                            )
                        }
                    )
                }
            },
            onComplete = onComplete
        )
    }


    override fun upload(
        request: UploadRequest,
        listener: UploadListener
    ): Cancellable {
        val wrapped = listener.clearingAuthOnUnauthorized()
        return startTransfer(wrapped::onError) { task ->
            fileUploader.upload(request, wrapped, task)
        }
    }


    override fun download(
        request: DownloadRequest,
        listener: DownloadListener
    ): Cancellable {
        val wrapped = listener.clearingAuthOnUnauthorized()
        return startTransfer(wrapped::onError) { task ->
            fileDownloader.download(request, wrapped, task)
        }
    }


    override fun registerDevice(
        pushToken: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.registerDevice(pushToken, callback)
            },
            onComplete = onComplete
        )
    }


    override fun endSession(onComplete: (Result<Unit>) -> Unit) {
        disconnect()
        synchronizer.reset()
        // Queued after the reset: realtime events already waiting on the
        // sync thread must not repopulate the cache of the ended session
        execution.sync { dialogFactory.clear() }
        authManager.endSession(onComplete)
    }


    override fun connect() {
        logger.debug(TAG, "called connect()")

        // Ordered after a preceding `endSession()` reset on the sync thread
        synchronizer.activate()

        if (realtimeEnabled) {
            logger.debug(TAG,
                "connect: realtime is enabled. State $connectionState"
            )
            return
        }

        realtimeEnabled = true
        retryAttempt = 0
        hub.updateState(ConnectionState.Connecting)

        authManager.ensureAuthValid { authResult ->
            authResult.fold(
                onSuccess = { realtime.connect() },
                onFailure = { error ->
                    if (error is ChatError.Unauthorized) {
                        handleUnauthorized(
                            false,
                            retry = {
                                authManager.refresh { authRefresh ->
                                    authRefresh.fold(
                                        onSuccess = { realtime.connect() },
                                        onFailure = { failRealtime(it.toChatError()) }
                                    )
                                }
                            },
                            fail = { failRealtime(error.toChatError()) }
                        )

                    } else {
                        failRealtime(error.toChatError())
                    }
                }
            )
        }
    }


    override fun disconnect() {
        realtimeEnabled = false
        backoffTask?.cancel()
        backoffTask = null
        retryAttempt = 0
        realtime.disconnect()
    }


    override fun addEventListener(listener: ChatEventListener) {
        hub.addGlobalListener(listener)
    }


    override fun removeEventListener(listener: ChatEventListener) {
        hub.removeGlobalListener(listener)
    }


    override fun addConnectionListener(listener: ConnectionListener) {
        hub.addConnectionListener(listener)
    }


    override fun removeConnectionListener(listener: ConnectionListener) {
        hub.removeConnectionListener(listener)
    }


    override fun addClientListener(listener: ChatClientListener) {
        hub.addClientListener(listener)
    }


    override fun removeClientListener(listener: ChatClientListener) {
        hub.removeClientListener(listener)
    }


    fun getHistory(
        dialogId: String,
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<Message>>) -> Unit
    ) {
        return callWithAuthRetry(
            call = { callback ->
                api.getHistory(dialogId, request) { resultDto ->

                    val mapped = resultDto.map { slice ->
                        val currentUserId = authManager.currentContact?.id
                        HistorySlice(
                            items = slice.items.map { it.toDomain(currentUserId) },
                            newerCursor = slice.newerCursor,
                            olderCursor = slice.olderCursor
                        )
                    }
                    callback(mapped)
                }
            },
            onComplete = onComplete
        )
    }


    override fun searchMessages(
        request: MessageSearchRequest,
        dialogId: String?,
        onComplete: (Result<MessageSearchSlice>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.searchMessages(dialogId, request) { resultDto ->

                    val mapped = resultDto.map { result ->
                        val currentUserId = authManager.currentContact?.id
                        MessageSearchSlice(
                            items = result.items.map { it.toDomain(currentUserId) },
                            newerCursor = result.newerCursor,
                            olderCursor = result.olderCursor
                        )
                    }
                    callback(mapped)
                }
            },
            onComplete = onComplete
        )
    }


    /**
     * Validates auth before starting a transfer. The returned task can be
     * cancelled immediately; if it was cancelled before auth completes, the
     * transfer is not started and [ChatError.Canceled] is reported instead.
     * Auth errors may be delivered on a different thread than transfer errors.
     */
    private fun startTransfer(
        onError: (ChatError) -> Unit,
        start: (TransferTaskImpl) -> Unit
    ): Cancellable {
        val task = TransferTaskImpl()

        authManager.ensureAuthValid { authResult ->
            if (task.isCanceled()) {
                onError(ChatError.Canceled)
                return@ensureAuthValid
            }

            authResult.fold(
                onSuccess = {
                    runCatching { start(task) }
                        .onFailure { error -> onError(error.toChatError()) }
                },
                onFailure = { error -> onError(error.toChatError()) }
            )
        }

        return task
    }


    /**
     * Transfers do not retry on 401: the stale token is cleared and the error
     * is passed to the listener as is.
     */
    private fun UploadListener.clearingAuthOnUnauthorized(): UploadListener {
        val origin = this
        return object : UploadListener by origin {
            override fun onError(error: ChatError) {
                if (error is ChatError.Unauthorized) authManager.clearAuth()
                origin.onError(error)
            }
        }
    }


    private fun DownloadListener.clearingAuthOnUnauthorized(): DownloadListener {
        val origin = this
        return object : DownloadListener by origin {
            override fun onError(error: ChatError) {
                if (error is ChatError.Unauthorized) authManager.clearAuth()
                origin.onError(error)
            }
        }
    }


    private fun realtimeListener(): RealtimeListener =
        object : RealtimeListener {
            override fun onMessage(message: MessageDto, unreadCount: Int?, cursor: String?) {
                synchronizer.submit(cursor) {
                    val currentUserId = authManager.currentContact?.id
                    val dialog = dialogFactory.get(message.dialogId)
                    val messageDomain = message.toDomain(currentUserId)
                    dialog?.applyMessage(message, unreadCount, currentUserId)

                    hub.dispatch(
                        MessageEvent.Received(message.dialogId, messageDomain)
                    )
                }
            }

            override fun onNewDialog(dialog: DialogDto, cursor: String?) {
                synchronizer.submit(cursor) {
                    val newDialog = dialogFactory.getOrCreate(dialog)
                    hub.dispatch(
                        DialogEvent.Created(newDialog.id, newDialog)
                    )
                }
            }

            // Ephemeral, never buffered by the synchronizer. Dispatched on the
            // sync thread so all chat events reach listeners from one thread.
            override fun onTyping(typing: TypingDto) {
                synchronizer.submitEphemeral {
                    hub.dispatch(
                        ActivityEvent.Typing(
                            dialogId = typing.dialogId,
                            member = typing.member.toDomain(),
                            previewText = typing.previewText,
                            timeoutMs = typing.timeoutMs
                        )
                    )
                }
            }

            override fun onMessageReaction(event: MessageReactionEventDto, cursor: String?) {
                synchronizer.submit(cursor) {
                    dialogFactory.get(event.dialogId)?.applyReactions(event.messageId, event.reactions)

                    hub.dispatch(
                        MessageEvent.ReactionsChanged(
                            dialogId = event.dialogId,
                            messageId = event.messageId,
                            reactions = event.reactions.map { it.toDomain() }
                        )
                    )
                }
            }

            override fun onMessageDeleted(event: MessageDeletedEventDto, cursor: String?) {
                synchronizer.submit(cursor) {
                    dialogFactory.get(event.dialogId)?.applyDeletion(event.messageId)

                    hub.dispatch(
                        MessageEvent.Deleted(
                            dialogId = event.dialogId,
                            deletion = MessageDeletion(
                                messageId = event.messageId,
                                deletedBy = event.deletedBy.toDomain(),
                                deletedAt = event.deletedAt
                            )
                        )
                    )
                }
            }

            override fun onMessageEdited(message: MessageDto, cursor: String?) {
                synchronizer.submit(cursor) {
                    val dialog = dialogFactory.get(message.dialogId)
                    val merged = dialog?.applyEdit(message) ?: message

                    hub.dispatch(
                        MessageEvent.Edited(
                            dialogId = message.dialogId,
                            message = merged.toDomain(authManager.currentContact?.id)
                        )
                    )
                }
            }

            override fun onMessageStatus(event: MessageStatusEventDto, cursor: String?) {
                synchronizer.submit(cursor) {
                    applyMessageStatus(event)
                }
            }

            override fun onConnectedEvent(cursor: String?) {
                synchronizer.onConnected(cursor)
            }

            override fun onError(error: ChatError) {
                if (!canRetry(retryAttempt)) {
                    failRealtime(error)
                    return
                }

                if (error is ChatError.Unauthorized) {
                    if (clientContext.autoRefreshAuth) {
                        refreshAuthAndReconnect()
                        return
                    }
                    failRealtime(error)
                    return
                }

                tryConnect()
            }

            override fun onOpen() {
                retryAttempt = 0
                hub.updateState(
                    ConnectionState.Connected
                )
            }

            override fun onClosed(code: Int, reason: String) {
                if (!realtimeEnabled) {
                    hub.updateState(ConnectionState.Disconnected)
                    return
                }

                if (!canRetry(retryAttempt)) {
                    closeRealtime(code, reason)
                    return
                }

                if (code == 401 || code == 1008) {
                    if (clientContext.autoRefreshAuth) {
                        refreshAuthAndReconnect()
                        return
                    }
                    closeRealtime(code, reason)
                    return
                }

                tryConnect()
            }
        }


    private fun applyMessageStatus(event: MessageStatusEventDto) {
        // TODO: map failed delivery statuses to `ReceiptEvent.DeliveryFailed`
        val kind = event.receiptKind
        if (kind == null) {
            logger.warn(TAG, "Unsupported message status: ${event.status}")
            return
        }

        val member = event.member.toDomain()

        // Unknown dialog: no local state to compare against, dispatch as is
        val advanced = dialogFactory.get(event.dialogId)
            ?.applyReceipt(member, kind, event.upToSeq, event.unreadCount)
            ?: true

        if (!advanced) return

        val receipt = when (kind) {
            ReceiptKind.DELIVERED -> ReceiptEvent.Delivered(event.dialogId, member, event.upToSeq)
            ReceiptKind.READ -> ReceiptEvent.Read(event.dialogId, member, event.upToSeq, event.unreadCount)
        }

        hub.dispatch(receipt)
    }


    override fun fetchUpdates(
        cursor: String,
        onComplete: (Result<UpdatesResponseDto>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.getUpdates(cursor, callback)
            },
            onComplete = onComplete
        )
    }


    override fun applyUpdates(threads: List<ThreadUpdatesDto>) {
        val currentUserId = currentUserId

        threads.forEach { thread ->
            val messageDtos = thread.messages.sortedBy { it.sequence ?: 0 }
            val messages = messageDtos.map { it.toDomain(currentUserId) }
            val memberChanges = thread.memberChanges.mapNotNull { it.toDomain() }

            val dialog: DialogImpl?

            if (thread.dialog != null) {
                val (resolved, isNew) = dialogFactory.getOrCreateReportingNew(thread.dialog)
                dialog = resolved

                if (thread.dialog.unreadCount == null) {
                    thread.unreadCount?.let(resolved::applyUnreadCount)
                }

                // Dialog created while offline: announce it before its changes
                if (isNew) {
                    hub.dispatch(DialogEvent.Created(resolved.id, resolved))
                }
            } else {
                dialog = dialogFactory.get(thread.threadId)
                dialog?.applySync(
                    lastMessage = thread.topMessage ?: messageDtos.lastOrNull(),
                    deletedMessageIds = thread.deletedMessageIds,
                    unreadCount = thread.unreadCount
                )

                // Before resolving read states, so newly added members are known
                dialog?.applyMemberChanges(memberChanges)
            }

            val members = thread.dialog?.members?.map { it.toDomain() }
                ?: dialog?.members
                ?: emptyList()
            val recoveredStates = thread.readStates.mapNotNull { it.toDomain(members) }

            // Expose the merged horizons, the same ones the dialog now holds
            val participantStates = dialog?.mergeParticipantStates(recoveredStates)
                ?: recoveredStates

            val changes = DialogSyncChanges(
                // Same value the dialog now holds
                unreadCount = dialog?.unreadCount ?: thread.unreadCount ?: 0,
                messages = messages,
                deletedMessageIds = thread.deletedMessageIds,
                participantStates = participantStates,
                // TODO: map once the server exposes delivery failures in updates
                deliveryExceptions = emptyList(),
                memberChanges = memberChanges,
                hasLeft = thread.left
            )

            hub.dispatch(DialogEvent.Synchronized(thread.threadId, changes))
        }
    }


    override fun resyncRequired() {
        hub.notifyResyncRequired()
    }


    private fun failRealtime(error: ChatError) {
        realtimeEnabled = false

        logger.error(
            "ChatClientImpl",
            "connect: Realtime connection failed. $error"
        )

        hub.updateState(ConnectionState.Failed(error))
    }


    private fun refreshAuthAndReconnect() {
        authManager.refresh { authResult ->
            if (!realtimeEnabled) return@refresh

            authResult.fold(
                onSuccess = { tryConnect() },
                onFailure = { failRealtime(it.toChatError()) }
            )
        }
    }


    private fun closeRealtime(code: Int, reason: String) {
        logger.error(TAG, "onClosed: close realtime")

        realtimeEnabled = false
        hub.updateState(
            ConnectionState.Failed(
                ChatError.fromCode(code, reason)
            )
        )
    }


    private fun tryConnect() {
        if (backoffTask != null) {
            logger.debug("ChatClientImpl",
                "connect: backoffTask is active"
            )
            return
        }

        retryAttempt++
        logger.debug("ChatClientImpl",
            "connect: retry open connection. Attempt $retryAttempt"
        )

        hub.updateState(
            ConnectionState.Reconnecting(retryAttempt,
                clientContext.networkConfig.realtime.maxRetries
            )
        )

        val delay = calculateBackoff(retryAttempt)
        logger.debug("ChatClientImpl",
            "connect: calculated backoff delay $delay"
        )
        backoffTask = Timer().schedule(delay) {
            backoffTask = null
            realtime.connect()
        }
    }


    private fun calculateBackoff(attempt: Int): Long =
        (clientContext.networkConfig.realtime.retryBaseDelayMs * 2.0.pow(attempt.toDouble()).toLong())
            .coerceAtMost(clientContext.networkConfig.realtime.maxRetryDelayMs)


    private fun canRetry(attempt: Int): Boolean {
        return realtimeEnabled &&
                attempt < clientContext.networkConfig.realtime.maxRetries
    }


    private fun <T> callWithAuthRetry(
        call: (onComplete: (Result<T>) -> Unit) -> Unit,
        onComplete: (Result<T>) -> Unit
    ) {
        var retried = false

        fun execute() {
            authManager.ensureAuthValid { authResult ->
                authResult.fold(
                    onSuccess = {
                        call { result ->
                            result.fold(
                                onSuccess = { onComplete(Result.success(it)) },
                                onFailure = { error ->
                                    if (error is ChatError.Unauthorized) {
                                        handleUnauthorized(
                                            retried,
                                            retry = {
                                                retried = true
                                                authManager.refresh { authRefresh ->
                                                    authRefresh.fold(
                                                        onSuccess = { execute() },
                                                        onFailure = { err ->
                                                            onComplete(Result.failure(err.toChatError()))
                                                        }
                                                    )
                                                }
                                            },
                                            fail = { onComplete(Result.failure(error.toChatError())) }
                                        )

                                    } else {
                                        onComplete(Result.failure(error.toChatError()))
                                    }
                                }
                            )
                        }
                    },

                    onFailure = { error ->
                        if (error is ChatError.Unauthorized) {
                            handleUnauthorized(
                                retried,
                                retry = {
                                    retried = true
                                    authManager.refresh { authRefresh ->
                                        authRefresh.fold(
                                            onSuccess = { execute() },
                                            onFailure = { onComplete(Result.failure(it.toChatError())) }
                                        )
                                    }
                                },
                                fail = { onComplete(Result.failure(error.toChatError())) }
                            )

                        } else {
                            onComplete(Result.failure(error.toChatError()))
                        }
                    }
                )
            }
        }

        execute()
    }


    private fun <T> callCancellableWithAuthRetry(
        call: (onComplete: (Result<T>) -> Unit) -> Cancellable,
        onComplete: (Result<T>) -> Unit
    ): Cancellable {

        val composite = CompositeCancellable()
        var retried = false

        fun execute() {
            if (composite.isCanceled()) return

            authManager.ensureAuthValid { authResult ->
                if (composite.isCanceled()) return@ensureAuthValid

                authResult.fold(
                    onSuccess = {
                        val c = call { result ->
                            if (composite.isCanceled()) return@call

                            result.fold(
                                onSuccess = { onComplete(Result.success(it)) },
                                onFailure = { error ->
                                    if (error is ChatError.Unauthorized) {
                                        handleUnauthorized(
                                            retried,
                                            retry = {
                                                retried = true
                                                authManager.refresh { authRefresh ->
                                                    if (composite.isCanceled()) return@refresh

                                                    authRefresh.fold(
                                                        onSuccess = { execute() },
                                                        onFailure = { err ->
                                                            onComplete(Result.failure(err.toChatError()))
                                                        }
                                                    )
                                                }
                                            },

                                            fail = { onComplete(Result.failure(error.toChatError())) }
                                        )

                                    } else {
                                        onComplete(Result.failure(error.toChatError()))
                                    }
                                }
                            )
                        }

                        composite.add(c)
                    },
                    onFailure = { error ->
                        if (error is ChatError.Unauthorized) {
                            handleUnauthorized(
                                retried,
                                retry = {
                                    retried = true
                                    authManager.refresh { authRefresh ->
                                        if (composite.isCanceled()) return@refresh

                                        authRefresh.fold(
                                            onSuccess = { execute() },
                                            onFailure = { err ->
                                                onComplete(Result.failure(err.toChatError()))
                                            }
                                        )
                                    }
                                },

                                fail = { onComplete(Result.failure(error.toChatError())) }
                            )
                        } else {
                            onComplete(Result.failure(error.toChatError()))
                        }
                    }
                )
            }
        }

        execute()
        return composite
    }


    private inline fun handleUnauthorized(
        retried: Boolean,
        crossinline retry: () -> Unit,
        crossinline fail: () -> Unit
    ) {
        authManager.clearAuth()
        if (!retried && clientContext.autoRefreshAuth) {
            retry()
        } else {
            fail()
        }
    }
}