package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageReplyDto(
    val messageId: String,
    val sender: ParticipantDto,
    val type: String,
    val body: String?,
    val createdAt: Long,
    val attachmentMime: String?,
    val attachmentName: String?,
    val attachmentAddress: String?,
    val isDeleted: Boolean
)
