package com.webitel.chat.sdk


/** Result of a [Dialog.forwardMessages]/[ChatClient.forwardMessages] call. */
data class ForwardMessagesResult(

    /** Identifiers of the newly created forwarded messages. */
    val ids: List<String>,

    /** Requested identifiers that were not forwarded, with the reason why. */
    val skipped: List<SkippedMessage>,

    /** Identifier of the destination dialog the messages were forwarded into. */
    val threadId: String
)
