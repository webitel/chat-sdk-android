package com.webitel.chat.sdk


/**
 * Information about the original source of a message that was forwarded.
 *
 * Present on [Message.forwardOrigin] when the message was forwarded either
 * from another Webitel chat, or from an external messenger.
 */
data class MessageForwardOrigin(

    /** When the original message was sent, before it was forwarded, in milliseconds since epoch. */
    val originalSentAt: Long,

    /** Specific kind of origin and the data that comes with it. */
    val kind: MessageForwardOriginKind
) {

    /** Display name of the original sender, if known. */
    val senderDisplayName: String?
        get() = kind.senderDisplayName
}


/**
 * Specific origin of a forwarded message.
 */
sealed class MessageForwardOriginKind {

    /** Forwarded from another chat within Webitel. */
    data class InternalUser(
        val senderId: String,
        val senderName: String?,
        val sourceMessageId: String
    ) : MessageForwardOriginKind()


    /** Forwarded from an external messenger, sender named. */
    data class ExternalUser(val senderName: String) : MessageForwardOriginKind()


    /** Forwarded from an external messenger; the sender chose to hide their identity. */
    object ExternalHiddenUser : MessageForwardOriginKind()


    /** Forwarded from an external group or channel rather than a person. */
    data class ExternalChat(val name: String?) : MessageForwardOriginKind()


    /** Origin kind not recognized by this SDK version. */
    data class Unsupported(val rawKind: String, val senderName: String?) : MessageForwardOriginKind()


    val senderDisplayName: String?
        get() = when (this) {
            is InternalUser -> senderName
            is ExternalUser -> senderName
            is ExternalHiddenUser -> null
            is ExternalChat -> name
            is Unsupported -> senderName
        }
}
