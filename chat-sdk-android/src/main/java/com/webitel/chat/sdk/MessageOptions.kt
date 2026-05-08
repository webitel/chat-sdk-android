package com.webitel.chat.sdk

import java.util.UUID


/**
 * Options used when sending a message.
 */
data class MessageOptions(
    /** Content of the message. */
    val content: SendContent,

    /** Client-generated identifier used to match sent messages. */
    val sendId: String = UUID.randomUUID().toString()
)