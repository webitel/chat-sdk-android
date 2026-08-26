package com.webitel.chat.sdk


/**
 * Describes a single message deletion event.
 *
 * Delivered as part of [MessageEvent.Deleted] and carries who deleted the
 * message and when, in addition to its identifier.
 */
data class MessageDeletion(

    /**
     * Identifier of the deleted message.
     */
    val messageId: String,

    /**
     * Participant who deleted the message.
     */
    val deletedBy: Participant,

    /**
     * Deletion timestamp in milliseconds since epoch.
     */
    val deletedAt: Long,
)
