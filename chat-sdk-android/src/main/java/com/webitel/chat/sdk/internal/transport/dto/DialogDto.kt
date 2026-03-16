package com.webitel.chat.sdk.internal.transport.dto

internal data class DialogDto(
    val id: String,
    val lastMessage: MessageDto?,
    val subject: String,
    val members: List<ContactDto>,
    val type: String,
)