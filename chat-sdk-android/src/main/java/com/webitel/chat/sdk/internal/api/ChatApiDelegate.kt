package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ContactId
import com.webitel.chat.sdk.ContactRequest
import com.webitel.chat.sdk.DialogRequest
import com.webitel.chat.sdk.EditMessageResult
import com.webitel.chat.sdk.ForwardMessagesResult
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.MessageAction
import com.webitel.chat.sdk.MessageDeletionResult
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageSearchRequest
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.Page
import com.webitel.chat.sdk.ReactionResult
import com.webitel.chat.sdk.TypingRequest
import com.webitel.chat.sdk.internal.transport.dto.ContactDto
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageSearchResultDto
import com.webitel.chat.sdk.internal.transport.dto.ReadPosition
import com.webitel.chat.sdk.internal.transport.dto.UpdatesResponseDto

internal interface ChatApiDelegate {

    fun sendMessage(
        target: MessageTarget,
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable

    fun getDialogs(
        request: DialogRequest,
        onComplete: (Result<Page<DialogDto>>) -> Unit
    )

    fun getOrCreateDialog(
        contactId: ContactId,
        onComplete: (Result<DialogDto>) -> Unit
    )

    fun getContacts(
        request: ContactRequest,
        onComplete: (Result<Page<ContactDto>>)-> Unit
    )

    fun getHistory(
        dialogId: String,
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<MessageDto>>) -> Unit
    )

    fun searchMessages(
        dialogId: String?,
        request: MessageSearchRequest,
        onComplete: (Result<MessageSearchResultDto>) -> Unit
    )

    fun registerDevice(
        pushToken: String,
        onComplete: (Result<Unit>) -> Unit
    )

    fun sendAction(
        messageId: String,
        action: MessageAction,
        onComplete: (Result<Unit>) -> Unit
    )

    fun sendTyping(
        dialogId: String,
        request: TypingRequest,
        onComplete: (Result<Unit>) -> Unit
    )

    fun markAsRead(
        dialogId: String,
        position: ReadPosition,
        onComplete: (Result<Unit>) -> Unit
    )

    fun setReaction(
        messageId: String,
        emoji: String,
        sendId: String?,
        onComplete: (Result<ReactionResult>) -> Unit
    )

    fun deleteMessages(
        ids: List<String>,
        onComplete: (Result<MessageDeletionResult>) -> Unit
    )

    fun forwardMessages(
        ids: List<String>,
        target: MessageTarget,
        sendId: String,
        onComplete: (Result<ForwardMessagesResult>) -> Unit
    )

    fun editMessage(
        messageId: String,
        text: String,
        onComplete: (Result<EditMessageResult>) -> Unit
    )

    fun getUpdates(
        cursor: String,
        onComplete: (Result<UpdatesResponseDto>) -> Unit
    )
}
