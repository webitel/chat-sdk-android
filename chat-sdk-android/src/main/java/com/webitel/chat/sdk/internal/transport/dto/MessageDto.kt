package com.webitel.chat.sdk.internal.transport.dto

import com.webitel.chat.sdk.MessageContent

internal data class MessageDto(
    val id: String,
    val dialogId: String,
    val createdAt: Long,
    val editedAt: Long?,
    val from: ParticipantDto,
    val content: MessageContent,
    val sendId: String?,
    val reactions: List<MessageReactionDto> = emptyList(),
    val replyTo: MessageReplyDto? = null,
    val forwardOrigin: MessageForwardOriginDto? = null,
    val sequence: Long? = null,
    val failures: List<DeliveryFailureDto> = emptyList(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedBy: ParticipantDto? = null,
)