package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageForwardOriginDto(
    val kind: String,
    val senderId: String?,
    val senderName: String?,
    val sourceMessageId: String?,
    val originalSentAt: Long
)
