package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageDto(
    val id: String,
    val dialogId: String,
    val createdAt: Long,
    val updatedAt: Long?,
    val from: ContactDto,
    val sendId: String?,
    val text: String?
)