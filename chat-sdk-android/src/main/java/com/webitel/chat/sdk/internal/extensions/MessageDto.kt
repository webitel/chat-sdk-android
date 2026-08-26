package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageReaction
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionDto

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
        sendId = sendId,
        reactions = reactions.map { it.toDomain() }
    )
}


internal fun MessageReactionDto.toDomain(): MessageReaction {
    return MessageReaction(
        emoji = emoji,
        count = count,
        reactedByMe = reactedByMe,
        reactorIds = reactorIds,
        lastReactedAt = lastReactedAt
    )
}