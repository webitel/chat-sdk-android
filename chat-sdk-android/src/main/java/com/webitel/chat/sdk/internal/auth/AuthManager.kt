package com.webitel.chat.sdk.internal.auth

import com.webitel.chat.sdk.internal.transport.dto.ContactDto

internal interface AuthManager {
    val currentContact: ContactDto?
    fun refresh(onComplete: (Result<Unit>) -> Unit)
    fun endSession(onComplete: (Result<Unit>) -> Unit)
}