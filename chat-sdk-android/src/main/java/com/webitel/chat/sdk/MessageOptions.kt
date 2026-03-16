package com.webitel.chat.sdk

import java.util.UUID


/**
 * Options used when sending a message.
 */
data class MessageOptions(

    /** Text content of the message. */
    val text: String? = null,

    /** Client-generated identifier used to match sent messages. */
    val sendId: String = UUID.randomUUID().toString()
) {
    init {
        require(!text.isNullOrEmpty()) {
            "Message must contain text or file"
        }
    }
}