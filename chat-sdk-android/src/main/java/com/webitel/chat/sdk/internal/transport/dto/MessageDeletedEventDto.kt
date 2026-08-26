package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageDeletedEventDto(
    val dialogId: String,
    val messageId: String,
    val deletedBy: ParticipantDto,
    val deletedAt: Long,
)
