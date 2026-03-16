package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ContactRequest
import com.webitel.chat.sdk.DialogRequest
import com.webitel.chat.sdk.DownloadListener
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.Page
import com.webitel.chat.sdk.internal.transport.dto.ContactDto
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto

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

    fun getContacts(
        request: ContactRequest,
        onComplete: (Result<Page<ContactDto>>)-> Unit
    )

    fun getHistory(
        dialogId: String,
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<MessageDto>>) -> Unit
    )

    fun downloadFile(
        dialogId: String,
        fileId: String,
        offset: Long?,
        listener: DownloadListener
    ): Cancellable

    fun registerDevice(
        pushToken: String,
        onComplete: (Result<Unit>) -> Unit
    )
}