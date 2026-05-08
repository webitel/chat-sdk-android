package com.webitel.chat.sdk


/**
 * Represents a chat message.
 *
 * A message encapsulates all data required to render a single item in a conversation.
 * Its content is defined by [MessageContent] and may include:
 * - text
 * - attachments
 * - an interactive keyboard
 * - or a combination of these
 */
data class Message(

    /**
     * Unique identifier of the message.
     */
    val id: String,

    /**
     * Identifier of the dialog this message belongs to.
     */
    val dialogId: String,

    /**
     * Message creation timestamp in milliseconds since epoch.
     */
    val createdAt: Long,

    /**
     * Last edit timestamp in milliseconds since epoch.
     */
    val editedAt: Long,

    /**
     * Sender of the message.
     */
    val from: Participant,

    /**
     * Client-generated identifier used to track message sending state.
     *
     * Typically used for:
     * - deduplication
     * - request tracking
     * - optimistic UI updates
     */
    val sendId: String? = null,

    /**
     * Indicates whether the message was sent by the current user.
     */
    val isOutgoing: Boolean,

    /**
     * Message content.
     *
     * See [MessageContent] for all supported content types and combinations.
     */
    val content: MessageContent
)