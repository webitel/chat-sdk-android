package com.webitel.chat.sdk

/**
 * Represents a participant in a dialog or conversation.
 *
 * A participant wraps a [Contact] with additional context,
 * such as their role within the dialog.
 */
data class Participant(
    /** Unique identifier of the participant within the dialog. */
    val id: String,

    /** Underlying contact information associated with this participant. */
    val contact: Contact,

    /** Role of the participant in the dialog. */
    val role: ParticipantRole
)