package com.webitel.chat.sdk


/**
 * Represents the destination where a message should be sent.
 *
 * Used by the SDK to determine whether the message should be delivered
 * to an existing dialog or directly to a contact (which may create
 * a dialog if it does not exist yet).
 */
sealed interface MessageTarget {

    /** Target an existing dialog by its unique identifier. */
    data class Dialog(val id: String) : MessageTarget

    /** Target a contact directly (a dialog may be created automatically). */
    data class Contact(val contactId: ContactId) : MessageTarget
}