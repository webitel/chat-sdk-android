package com.webitel.chat.sdk


/**
 * Reference to another message that the current message replies to.
 *
 * Provides a lightweight preview of the original message — enough to
 * render a reply-quote in the UI without fetching the full message.
 */
data class MessageReply(

    /** Identifier of the original message being replied to. */
    val messageId: String,

    /** Sender of the original message. */
    val from: Participant,

    /** Creation timestamp of the original message, in milliseconds since epoch. */
    val createdAt: Long,

    /**
     * Whether the original message has since been deleted.
     *
     * When `true`, [content] still carries a text preview (if any),
     * but attachment/location metadata is stripped by the server.
     */
    val isDeleted: Boolean,

    /** Preview of the original message content. */
    val content: MessageReplyContent
)


/**
 * Lightweight preview of the content of a replied-to message.
 *
 * This is a reduced projection of [MessageContent], sufficient for
 * rendering a reply-quote (e.g. a single line above the current message).
 */
sealed class MessageReplyContent {

    /** Plain text preview. */
    data class Text(val text: String?) : MessageReplyContent()


    /** Image attachment preview. */
    data class Image(
        val caption: String?,
        val mimeType: String?
    ) : MessageReplyContent()


    /** Document/file attachment preview. */
    data class Document(
        val name: String?,
        val mimeType: String?,
        val caption: String?
    ) : MessageReplyContent()


    /** Location preview. */
    data class Location(
        val name: String?,
        val address: String?
    ) : MessageReplyContent()


    /** Contact card preview. */
    data class Contact(
        val displayValue: String?
    ) : MessageReplyContent()


    /** System event preview. */
    data class System(val text: String) : MessageReplyContent()


    /** Interactive (keyboard) message preview. */
    data class Interactive(val text: String?) : MessageReplyContent()


    /** Preview for a content type not (yet) recognized by the SDK. */
    data class Unsupported(
        val type: String,
        val text: String?
    ) : MessageReplyContent()
}
