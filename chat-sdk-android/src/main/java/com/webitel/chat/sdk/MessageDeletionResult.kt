package com.webitel.chat.sdk


/** Result of a [Dialog.deleteMessages]/[ChatClient.deleteMessages] call. */
data class MessageDeletionResult(

    /** Timestamp of the deletion, in milliseconds since epoch. */
    val deletedAt: Long?,

    /** Identifiers of the messages that were actually deleted. */
    val deletedIds: List<String>,

    /** Requested identifiers that were not deleted, with the reason why. */
    val skipped: List<SkippedMessage>,
)
