package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.DialogType
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto

internal class DialogFactory(
    private val client: ChatClientImpl,
    private val realtimeHub: RealtimeHub
) {
    /** Accessed from the sync thread and API callbacks. Access only under [lock]. */
    private val lock = Any()
    private val cache = mutableMapOf<String, DialogImpl>()

    fun getOrCreate(dialogInfo: DialogDto): DialogImpl =
        getOrCreateReportingNew(dialogInfo).first


    /**
     * Same as [getOrCreate], but also reports whether the dialog
     * was created by this call. The check and insertion are atomic.
     */
    fun getOrCreateReportingNew(dialogInfo: DialogDto): Pair<DialogImpl, Boolean> =
        synchronized(lock) {
            val existing = cache[dialogInfo.id]

            if (existing != null) {
                existing.update(dialogInfo)
                return existing to false
            }

            val dialog = DialogImpl(
                id = dialogInfo.id,
                client = client,
                hub = realtimeHub,
                type = DialogType.from(dialogInfo.type),
                members = dialogInfo.members.map { it.toDomain() },
                snapshot = dialogInfo
            )

            cache[dialogInfo.id] = dialog
            dialog to true
        }


    fun get(dialogId: String): DialogImpl? = synchronized(lock) { cache[dialogId] }


    /** Forgets dialogs of the ended session. */
    fun clear() = synchronized(lock) { cache.clear() }
}
