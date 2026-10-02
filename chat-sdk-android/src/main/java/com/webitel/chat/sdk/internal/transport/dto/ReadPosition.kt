package com.webitel.chat.sdk.internal.transport.dto

/**
 * Position in a dialog up to which messages are marked as read.
 *
 * Exactly one of `id` or `up_to_seq` is sent; the server treats them equally.
 */
internal sealed class ReadPosition {
    data class Sequence(val sequence: Long) : ReadPosition()
    data class MessageId(val messageId: String) : ReadPosition()
}
