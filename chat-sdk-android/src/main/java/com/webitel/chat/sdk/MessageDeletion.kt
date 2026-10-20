package com.webitel.chat.sdk


/**
 * Who deleted a message and when.
 *
 * Set on [Message.deletion] for deleted messages and delivered as part of
 * [MessageEvent.Deleted] and [DialogSyncChanges.deletedMessages].
 */
data class MessageDeletion(

    /**
     * Participant who deleted the message.
     */
    val deletedBy: Participant,

    /**
     * Deletion timestamp in milliseconds since epoch.
     */
    val deletedAt: Long,
)
