package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.ChatError

internal fun Throwable.toChatError(): ChatError =
    ChatError.fromCode(ChatError.UNKNOWN_CODE, message, this)