package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatEventListener
import com.webitel.chat.sdk.Dialog
import com.webitel.chat.sdk.DialogType
import com.webitel.chat.sdk.EditMessageResult
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageAction
import com.webitel.chat.sdk.MessageDeletionResult
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.Participant
import com.webitel.chat.sdk.ReactionResult
import com.webitel.chat.sdk.TypingRequest
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionDto

internal class DialogImpl(
    private val client: ChatClientImpl,
    private val hub: RealtimeHub,
    override val id: String,
    override val type: DialogType,
    override var members: List<Participant>,
    private var snapshot: DialogDto
) : Dialog {

    override val subject: String
        get() {
            return snapshot.subject
        }

    override val lastMessage: Message?
        get() {
            return snapshot.lastMessage
                ?.toDomain(client.currentUserId)
        }


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


    override fun addListener(listener: ChatEventListener) {
        hub.addDialogListener(id, listener)
    }


    override fun removeListener(listener: ChatEventListener) {
        hub.removeDialogListener(id, listener)
    }


    internal fun update(info: DialogDto) {
        snapshot = info
    }


    internal fun applyMessage(message: MessageDto) {
        val current = snapshot

        snapshot = current.copy(
            lastMessage = message
        )
    }


    internal fun applyReactions(messageId: String, reactions: List<MessageReactionDto>) {
        val current = snapshot
        val last = current.lastMessage ?: return
        if (last.id != messageId) return

        snapshot = current.copy(
            lastMessage = last.copy(reactions = reactions)
        )
    }


    internal fun applyDeletion(messageId: String) {
        val current = snapshot
        val last = current.lastMessage ?: return
        if (last.id != messageId) return

        snapshot = current.copy(lastMessage = null)
    }


    internal fun applyEdit(message: MessageDto): MessageDto {
        val current = snapshot
        val last = current.lastMessage

        val merged = if (last?.id == message.id && message.reactions.isEmpty()) {
            message.copy(reactions = last.reactions)
        } else {
            message
        }

        if (last?.id == message.id) {
            snapshot = current.copy(lastMessage = merged)
        }

        return merged
    }
}