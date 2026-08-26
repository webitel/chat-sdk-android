package com.webitel.chat.sdk.internal.transport.dto

internal data class TypingDto(
    val dialogId: String,
    val member: ParticipantDto,
    val previewText: String?,
    val timeoutMs: Long?
)
