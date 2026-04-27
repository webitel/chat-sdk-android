package com.webitel.chat.sdk


/**
 * Represents a chat message.
 *
 * A message may contain text, attachments, or both.
 * Attachments contain metadata only — binary data must be downloaded explicitly.
 */
data class Message(

    /** Unique message identifier */
    val id: String,

    /** Dialog identifier */
    val dialogId: String,

    /** Message creation timestamp (milliseconds since epoch) */
    val createdAt: Long,

    /**
     * Last edit timestamp (milliseconds since epoch).
     *
     * Null if the message has never been edited.
     */
    val editedAt: Long?,

    /** Sender of the message */
    val from: Participant,

    /**
     * Text content of the message.
     *
     * May be null when the message contains only attachments
     * (for example, image or file messages).
     */
    val text: String?,

    /**
     * Client-generated request ID for the message.
     * This ID is used to track the message request from the client side.
     */
    val sendId: String? = null,

    /**
     * Indicates whether the message is incoming or outgoing.
     */
    val isOutgoing: Boolean,

    /**
     * List of attachments associated with this message.
     *
     * Attachments provide metadata only.
     * Use their `fileId` to download the actual content.
     *
     * Example:
     * ```
     * message.attachments.forEach { attachment ->
     *     when (attachment) {
     *         is MessageAttachment.Image -> showImagePreview(attachment)
     *         is MessageAttachment.Video -> showVideoPreview(attachment)
     *     }
     * }
     * ```
     */
    val attachments: List<MessageAttachment> = emptyList()
)
