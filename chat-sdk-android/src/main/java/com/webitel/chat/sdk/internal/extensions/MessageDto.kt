package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.internal.transport.dto.MessageDto

internal fun MessageDto.toDomain(
    currentUserId: String?
): Message {

    val from = from.toDomain()

    val isOutgoing = from.contact.id.sub == currentUserId

    return Message(
        id = id,
        dialogId = dialogId,
        createdAt = createdAt,
        editedAt = editedAt,
        from = from,
        content = content,
        isOutgoing = isOutgoing,
        sendId = sendId
    )
}