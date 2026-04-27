package com.webitel.chat.sdk.internal.transport.dto

internal data class ParticipantDto (
    val id: String,
    val contact: ContactDto,
    val role: String
)