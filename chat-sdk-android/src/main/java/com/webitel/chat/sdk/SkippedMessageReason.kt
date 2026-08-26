package com.webitel.chat.sdk


/** Reason a message id was not deleted, reported in [SkippedMessage.reason]. */
enum class SkippedMessageReason(val value: String) {

    /** The message no longer exists. */
    NOT_FOUND("REASON_NOT_FOUND"),

    /** The current user is not the author of the message. */
    NOT_AUTHOR("REASON_NOT_AUTHOR"),

    /** The message was already deleted. */
    ALREADY_DELETED("REASON_ALREADY_DELETED"),

    /** The dialog the message belongs to is closed. */
    CHAT_CLOSED("REASON_CHAT_CLOSED"),

    /** The current user is not allowed to delete this message. */
    NOT_ALLOWED("REASON_NOT_ALLOWED"),

    /** Unrecognized value received from the server. */
    UNKNOWN("");

    companion object {
        fun from(value: String): SkippedMessageReason =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
