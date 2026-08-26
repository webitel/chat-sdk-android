package com.webitel.chat.sdk


/** A message id that was not deleted, with the reason why. */
data class SkippedMessage(
    val id: String,
    val reason: SkippedMessageReason,
)
