package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageDeletion
import com.webitel.chat.sdk.MessageForwardOrigin
import com.webitel.chat.sdk.MessageForwardOriginKind
import com.webitel.chat.sdk.MessageReaction
import com.webitel.chat.sdk.MessageReply
import com.webitel.chat.sdk.MessageReplyContent
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageForwardOriginDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReplyDto

internal fun MessageDto.toDomain(
    currentUserId: String?
): Message {

    val from = from.toDomain()

    val isOutgoing = from.contact.id.sub == currentUserId

    // Deleted messages stay in the history; fall back to the sender
    // and the last change time if the server omits deletion details
    val deletion = if (isDeleted) {
        MessageDeletion(
            deletedBy = deletedBy?.toDomain() ?: from,
            deletedAt = deletedAt ?: editedAt ?: createdAt
        )
    } else {
        null
    }

    return Message(
        id = id,
        dialogId = dialogId,
        createdAt = createdAt,
        editedAt = editedAt,
        from = from,
        content = content,
        isOutgoing = isOutgoing,
        sendId = sendId,
        reactions = reactions.map { it.toDomain() },
        reply = replyTo?.toDomain(),
        forwardOrigin = forwardOrigin?.toDomain(),
        sequence = sequence,
        deliveryExceptions = failures.map { it.toDomain(id) },
        deletion = deletion
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


internal fun MessageReplyDto.toDomain(): MessageReply {
    return MessageReply(
        messageId = messageId,
        from = sender.toDomain(),
        createdAt = createdAt,
        isDeleted = isDeleted,
        content = toReplyContent()
    )
}


private fun MessageReplyDto.toReplyContent(): MessageReplyContent = when (type) {
    "text" -> MessageReplyContent.Text(body)

    "image" -> MessageReplyContent.Image(
        caption = body,
        mimeType = attachmentMime
    )

    "document" -> MessageReplyContent.Document(
        name = attachmentName,
        mimeType = attachmentMime,
        caption = body
    )

    "location" -> MessageReplyContent.Location(
        name = attachmentName,
        address = attachmentAddress
    )

    "contact" -> MessageReplyContent.Contact(displayValue = attachmentName)

    "system" -> MessageReplyContent.System(body.orEmpty())

    "interactive" -> MessageReplyContent.Interactive(body)

    else -> MessageReplyContent.Unsupported(type = type, text = body)
}


internal fun MessageForwardOriginDto.toDomain(): MessageForwardOrigin {
    return MessageForwardOrigin(
        originalSentAt = originalSentAt,
        kind = toForwardOriginKind()
    )
}


private fun MessageForwardOriginDto.toForwardOriginKind(): MessageForwardOriginKind = when (kind) {
    "FORWARD_ORIGIN_KIND_INTERNAL" -> MessageForwardOriginKind.InternalUser(
        senderId = senderId.orEmpty(),
        senderName = senderName,
        sourceMessageId = sourceMessageId.orEmpty()
    )

    "FORWARD_ORIGIN_KIND_EXTERNAL_USER" -> MessageForwardOriginKind.ExternalUser(
        senderName = senderName.orEmpty()
    )

    "FORWARD_ORIGIN_KIND_EXTERNAL_HIDDEN_USER" -> MessageForwardOriginKind.ExternalHiddenUser

    "FORWARD_ORIGIN_KIND_EXTERNAL_CHAT" -> MessageForwardOriginKind.ExternalChat(name = senderName)

    else -> MessageForwardOriginKind.Unsupported(rawKind = kind, senderName = senderName)
}