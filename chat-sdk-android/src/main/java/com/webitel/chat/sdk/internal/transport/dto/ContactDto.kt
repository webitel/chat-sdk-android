package com.webitel.chat.sdk.internal.transport.dto


internal data class ContactDto(
    val id: String,
    val iss: String,
    val name: String,
    val source: String,
    val isBot: Boolean
)