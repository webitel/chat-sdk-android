package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatEventListener
import com.webitel.chat.sdk.Contact
import com.webitel.chat.sdk.Dialog
import com.webitel.chat.sdk.DialogType
import com.webitel.chat.sdk.DownloadListener
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto

internal class DialogImpl(
    private val client: ChatClientImpl,
    private val hub: RealtimeHub,
    override val id: String,
    override val type: DialogType,
    override var members: List<Contact>,
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


    override fun getHistory(
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<Message>>) -> Unit
    ) {
        client.getHistory(id, request, onComplete)
    }


    override fun downloadFile(
        fileId: String,
        listener: DownloadListener
    ): Cancellable {
        return client.downloadFile(id, fileId, null, listener)
    }


    override fun downloadFile(
        fileId: String,
        offset: Long,
        listener: DownloadListener
    ): Cancellable {
        return client.downloadFile(id, fileId, offset, listener)
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
}