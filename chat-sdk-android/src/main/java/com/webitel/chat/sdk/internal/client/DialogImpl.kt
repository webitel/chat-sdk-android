package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatEventListener
import com.webitel.chat.sdk.DeliveryException
import com.webitel.chat.sdk.Dialog
import com.webitel.chat.sdk.DialogType
import com.webitel.chat.sdk.EditMessageResult
import com.webitel.chat.sdk.ForwardMessagesResult
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.MemberChange
import com.webitel.chat.sdk.MemberChangeAction
import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageAction
import com.webitel.chat.sdk.MessageDeletionResult
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageSearchRequest
import com.webitel.chat.sdk.MessageSearchSlice
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.Participant
import com.webitel.chat.sdk.ParticipantState
import com.webitel.chat.sdk.ReactionResult
import com.webitel.chat.sdk.TypingRequest
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionDto
import com.webitel.chat.sdk.internal.transport.dto.ReadPosition
import com.webitel.chat.sdk.internal.transport.dto.ReceiptKind

internal class DialogImpl(
    private val client: ChatClientImpl,
    private val hub: RealtimeHub,
    override val id: String,
    override val type: DialogType,
    members: List<Participant>,
    snapshot: DialogDto
) : Dialog {

    /**
     * Mutated from the sync thread and API callbacks, read from any thread.
     * Access only under [lock].
     */
    private val lock = Any()
    private var snapshot: DialogDto = snapshot
    private var currentMembers: List<Participant> = members
    private var states: List<ParticipantState> =
        snapshot.readStates.mapNotNull { it.toDomain(members) }
    private var exceptions: List<DeliveryException> = emptyList()

    override val subject: String
        get() = synchronized(lock) { snapshot.subject }

    override val members: List<Participant>
        get() = synchronized(lock) { currentMembers }

    override val lastMessage: Message?
        get() = synchronized(lock) { snapshot.lastMessage }
            ?.toDomain(client.currentUserId)

    override val participantStates: List<ParticipantState>
        get() = synchronized(lock) { states }

    override val deliveryExceptions: List<DeliveryException>
        get() = synchronized(lock) { exceptions }


    override fun sendMessage(
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable {
        return client.sendMessage(MessageTarget.Dialog(id), options, onComplete)
    }


    override fun sendAction(
        messageId: String,
        action: MessageAction,
        onComplete: (Result<Unit>) -> Unit
    ) {
        client.sendAction(messageId, action, onComplete)
    }


    override fun sendTyping(
        request: TypingRequest,
        onComplete: (Result<Unit>) -> Unit
    ) {
        client.sendTyping(id, request, onComplete)
    }


    override fun markAsRead(
        sequence: Long,
        onComplete: (Result<Unit>) -> Unit
    ) {
        client.markAsRead(id, ReadPosition.Sequence(sequence), onComplete)
    }


    override fun markAsRead(
        messageId: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        client.markAsRead(id, ReadPosition.MessageId(messageId), onComplete)
    }


    override fun setReaction(
        messageId: String,
        emoji: String,
        sendId: String?,
        onComplete: (Result<ReactionResult>) -> Unit
    ) {
        client.setReaction(messageId, emoji, sendId, onComplete)
    }


    override fun deleteMessages(
        ids: List<String>,
        onComplete: (Result<MessageDeletionResult>) -> Unit
    ) {
        client.deleteMessages(ids, onComplete)
    }


    override fun forwardMessages(
        ids: List<String>,
        sendId: String,
        onComplete: (Result<ForwardMessagesResult>) -> Unit
    ) {
        client.forwardMessages(ids, MessageTarget.Dialog(id), sendId, onComplete)
    }


    override fun editMessage(
        messageId: String,
        text: String,
        onComplete: (Result<EditMessageResult>) -> Unit
    ) {
        client.editMessage(messageId, text, onComplete)
    }


    override fun getHistory(
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<Message>>) -> Unit
    ) {
        client.getHistory(id, request, onComplete)
    }


    override fun searchMessages(
        request: MessageSearchRequest,
        onComplete: (Result<MessageSearchSlice>) -> Unit
    ) {
        client.searchMessages(request, id, onComplete)
    }


    override fun addListener(listener: ChatEventListener) {
        hub.addDialogListener(id, listener)
    }


    override fun removeListener(listener: ChatEventListener) {
        hub.removeDialogListener(id, listener)
    }


    internal fun update(info: DialogDto) {
        val members = info.members.map { it.toDomain() }
        val readStates = info.readStates.mapNotNull { it.toDomain(members) }

        synchronized(lock) {
            snapshot = info
            currentMembers = members
            states = merge(states, readStates)
        }
    }


    /**
     * Advances the participant's receipt horizon.
     *
     * @return `true` if the horizon moved forward, `false` for stale or duplicate receipts.
     */
    internal fun applyReceipt(
        member: Participant,
        kind: ReceiptKind,
        upToSequence: Long
    ): Boolean = synchronized(lock) {
        val current = states.firstOrNull { it.member.id == member.id }
            ?: ParticipantState(member, deliveredUpToSequence = 0, readUpToSequence = 0)

        val updated = when (kind) {
            ReceiptKind.DELIVERED -> {
                if (upToSequence <= current.deliveredUpToSequence) return false
                current.copy(member = member, deliveredUpToSequence = upToSequence)
            }

            ReceiptKind.READ -> {
                if (upToSequence <= current.readUpToSequence) return false
                current.copy(member = member, readUpToSequence = upToSequence)
            }
        }

        states = states.filterNot { it.member.id == member.id } + updated
        true
    }


    /**
     * Merges recovered horizons without moving any of them backwards.
     *
     * @return Resulting states of the participants present in [incoming].
     */
    internal fun mergeParticipantStates(
        incoming: List<ParticipantState>
    ): List<ParticipantState> = synchronized(lock) {
        states = merge(states, incoming)

        val ids = incoming.map { it.member.id }.toSet()
        states.filter { it.member.id in ids }
    }


    internal fun applyMemberChanges(changes: List<MemberChange>) {
        synchronized(lock) {
            val members = currentMembers.toMutableList()

            for (change in changes) {
                val index = members.indexOfFirst { it.id == change.member.id }

                when (change.action) {
                    MemberChangeAction.Added ->
                        if (index >= 0) members[index] = change.member
                        else members += change.member

                    MemberChangeAction.Removed ->
                        if (index >= 0) members.removeAt(index)

                    is MemberChangeAction.Unknown -> continue
                }
            }

            currentMembers = members
        }
    }


    internal fun applyMessage(message: MessageDto) {
        synchronized(lock) {
            snapshot = snapshot.copy(lastMessage = message)
        }
    }


    internal fun applyReactions(messageId: String, reactions: List<MessageReactionDto>) {
        synchronized(lock) {
            val last = snapshot.lastMessage ?: return
            if (last.id != messageId) return

            snapshot = snapshot.copy(
                lastMessage = last.copy(reactions = reactions)
            )
        }
    }


    internal fun applyDeletion(messageId: String) {
        synchronized(lock) {
            val last = snapshot.lastMessage ?: return
            if (last.id != messageId) return

            snapshot = snapshot.copy(lastMessage = null)
        }
    }


    internal fun applySync(lastMessage: MessageDto?, deletedMessageIds: List<String>) {
        synchronized(lock) {
            val current = snapshot.lastMessage

            if (current != null && current.id in deletedMessageIds) {
                snapshot = snapshot.copy(lastMessage = null)
            }

            if (lastMessage == null || lastMessage.id in deletedMessageIds) return

            val latest = snapshot.lastMessage
            if (latest != null && isNewer(latest, lastMessage)) return

            snapshot = snapshot.copy(lastMessage = lastMessage)
        }
    }


    /** Compares by sequence when both have it, by creation time otherwise. */
    private fun isNewer(lhs: MessageDto, rhs: MessageDto): Boolean {
        val lhsSequence = lhs.sequence
        val rhsSequence = rhs.sequence

        if (lhsSequence != null && rhsSequence != null) {
            return lhsSequence > rhsSequence
        }

        return lhs.createdAt > rhs.createdAt
    }


    internal fun applyEdit(message: MessageDto): MessageDto {
        synchronized(lock) {
            if (snapshot.lastMessage?.id == message.id) {
                snapshot = snapshot.copy(lastMessage = message)
            }
        }

        return message
    }


    private fun merge(
        current: List<ParticipantState>,
        incoming: List<ParticipantState>
    ): List<ParticipantState> {
        val result = current.toMutableList()

        for (item in incoming) {
            val index = result.indexOfFirst { it.member.id == item.member.id }

            if (index < 0) {
                result += item
                continue
            }

            val existing = result[index]
            result[index] = ParticipantState(
                member = item.member,
                deliveredUpToSequence = maxOf(existing.deliveredUpToSequence, item.deliveredUpToSequence),
                readUpToSequence = maxOf(existing.readUpToSequence, item.readUpToSequence)
            )
        }

        return result
    }
}
