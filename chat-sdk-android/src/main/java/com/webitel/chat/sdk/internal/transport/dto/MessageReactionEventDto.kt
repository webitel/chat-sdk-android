package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageReactionEventDto(
    val dialogId: String,
    val messageId: String,
    val reactions: List<MessageReactionDto>
)
