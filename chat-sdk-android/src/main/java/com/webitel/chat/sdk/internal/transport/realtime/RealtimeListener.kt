package com.webitel.chat.sdk.internal.transport.realtime

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto

internal interface RealtimeListener {
    fun onMessage(message: MessageDto)
    fun onNewDialog(dialog: DialogDto)
    fun onError(error: ChatError)
    fun onOpen()
    fun onClosed(code: Int, reason: String)
}