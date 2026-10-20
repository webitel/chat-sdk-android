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
     * Last edit timestamp in milliseconds since epoch, or `null` if the
     * message was never edited.
     */
    val editedAt: Long? = null,

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
    val content: MessageContent,

    /**
     * Aggregated reactions on this message, one entry per distinct emoji.
     *
     * Kept up to date via [MessageEvent.ReactionsChanged] while realtime is active.
     */
    val reactions: List<MessageReaction> = emptyList(),

    /**
     * Reference to the message this message replies to, or `null` if this
     * message is not a reply.
     */
    val reply: MessageReply? = null,

    /**
     * Original source if this message was forwarded, or `null` otherwise.
     */
    val forwardOrigin: MessageForwardOrigin? = null,

    /**
     * Position of the message within its dialog, or `null` if not provided
     * by the server.
     *
     * Compare with [ParticipantState.readUpToSequence] and [ReceiptEvent]
     * sequences to resolve delivery/read state.
     */
    val sequence: Long? = null,

    /**
     * Members this message failed to be delivered to, empty if no failures are known.
     *
     * See [ReceiptEvent.DeliveryFailed] for failures received in realtime.
     */
    val deliveryExceptions: List<DeliveryException> = emptyList(),

    /**
     * Who deleted the message and when, or `null` if the message is not deleted.
     *
     * Deleted messages stay in the history. See [MessageEvent.Deleted] for
     * deletions received in realtime.
     */
    val deletion: MessageDeletion? = null
) {

    /**
     * Indicates whether the message was edited after creation.
     *
     * Always `false` for deleted messages, since deletion also bumps [editedAt].
     */
    val isEdited: Boolean
        get() = !isDeleted && editedAt != null && editedAt > createdAt

    /**
     * Indicates whether the message was deleted.
     */
    val isDeleted: Boolean
        get() = deletion != null
}