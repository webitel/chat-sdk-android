package com.webitel.chat.sdk


/**
 * Listener used to receive chat-related events from the SDK.
 */
interface ChatEventListener {

    /** Called when a new [ChatEvent] is emitted. */
    fun onEvent(event: ChatEvent)
}
