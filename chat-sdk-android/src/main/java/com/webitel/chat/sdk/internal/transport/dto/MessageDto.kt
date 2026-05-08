package com.webitel.chat.sdk.internal.transport.dto

import com.webitel.chat.sdk.MessageContent

internal data class MessageDto(
    val id: String,
    val dialogId: String,
    val createdAt: Long,
    val editedAt: Long,
    val from: ParticipantDto,
    val content: MessageContent,
    val sendId: String?,
)