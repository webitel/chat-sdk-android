package com.webitel.chat.sdk


/**
 * Parameters for sending a typing indicator via [Dialog.sendTyping].
 */
data class TypingRequest(
    /** Preview of the text currently being composed. */
    val previewText: String? = null,

    /** How long the typing indicator should remain active, in milliseconds. */
    val timeoutMs: Long? = null
)
