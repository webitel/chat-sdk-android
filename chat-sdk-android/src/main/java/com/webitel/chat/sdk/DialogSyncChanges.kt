package com.webitel.chat.sdk


/**
 * Changes of a single dialog recovered after the realtime
 * connection was temporarily unavailable.
 *
 * Delivered via [DialogEvent.Synchronized].
 */
data class DialogSyncChanges(

    /** Current unread message count for the dialog. */
    val unreadCount: Int,

    /**
     * Messages whose server state changed while the client
     * was out of sync, sorted by [Message.sequence].
     *
     * Messages should be upserted by [Message.id].
     */
    val messages: List<Message>,

    /**
     * Messages deleted while the client was out of sync: who deleted each one
     * and when, keyed by message id.
     *
     * Apply it to the messages you have locally, e.g. by setting [Message.deletion].
     */
    val deletedMessages: Map<String, MessageDeletion>,

    /**
     * Current delivery/read horizons for affected participants.
     *
     * Same merged values as [Dialog.participantStates].
     */
    val participantStates: List<ParticipantState>,

    /** Members added or removed while the client was out of sync. */
    val memberChanges: List<MemberChange>,

    /** Indicates that the current user has left the dialog. */
    val hasLeft: Boolean
)


/**
 * Delivery/read horizons of a dialog participant.
 *
 * Horizons are cumulative: every message with
 * `Message.sequence <= readUpToSequence` is read by [member].
 */
data class ParticipantState(

    val member: Participant,

    /** Highest message sequence known to be delivered to this participant. */
    val deliveredUpToSequence: Long,

    /** Highest message sequence known to be read by this participant. */
    val readUpToSequence: Long
)


/**
 * Membership change of a dialog.
 */
data class MemberChange(

    val action: MemberChangeAction,

    /** Member affected by the change. */
    val member: Participant,

    /** Member who performed the change, if known. */
    val by: Participant?
)


sealed class MemberChangeAction {

    object Added : MemberChangeAction()

    object Removed : MemberChangeAction()

    /** Action not recognized by this SDK version. Contains the raw server value. */
    data class Unknown(val raw: String) : MemberChangeAction()
}
