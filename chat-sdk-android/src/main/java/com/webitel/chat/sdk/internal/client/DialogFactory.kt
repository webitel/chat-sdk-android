package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.DialogType
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto

internal class DialogFactory(
    private val client: ChatClientImpl,
    private val realtimeHub: RealtimeHub
) {
    private val cache = mutableMapOf<String, DialogImpl>()

    fun getOrCreate(dialogInfo: DialogDto): DialogImpl {
        val existing = cache[dialogInfo.id]

        return if (existing == null) {
            val dialog = DialogImpl(
                id = dialogInfo.id,
                client = client,
                hub = realtimeHub,
                type = DialogType.from(dialogInfo.type),
                members = dialogInfo.members.map { it.toDomain() },
                snapshot = dialogInfo
            )

            cache[dialogInfo.id] = dialog
            dialog
        } else {
            existing.update(dialogInfo)
            existing
        }
    }


    fun get(dialogId: String) = cache[dialogId]
}