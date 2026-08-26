package com.webitel.chat.sdk.internal.transport.realtime

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDeletedEventDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionEventDto
import com.webitel.chat.sdk.internal.transport.dto.TypingDto

internal interface RealtimeListener {
    fun onMessage(message: MessageDto)
    fun onNewDialog(dialog: DialogDto)
    fun onTyping(typing: TypingDto)
    fun onMessageReaction(event: MessageReactionEventDto)
    fun onMessageDeleted(event: MessageDeletedEventDto)
    fun onMessageEdited(message: MessageDto)
    fun onError(error: ChatError)
    fun onOpen()
    fun onClosed(code: Int, reason: String)
}