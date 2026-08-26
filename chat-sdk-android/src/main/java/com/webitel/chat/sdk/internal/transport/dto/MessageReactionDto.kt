package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageReactionDto(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean,
    val reactorIds: List<String>,
    val lastReactedAt: Long?
)
