package com.webitel.chat.sdk.internal.transport.realtime

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDeletedEventDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.dto.MessageReactionEventDto
import com.webitel.chat.sdk.internal.transport.dto.MessageStatusEventDto
import com.webitel.chat.sdk.internal.transport.dto.TypingDto

/**
 * `cursor` is the updates cursor carried by the frame, if any.
 */
internal interface RealtimeListener {
    fun onMessage(message: MessageDto, cursor: String?)
    fun onNewDialog(dialog: DialogDto, cursor: String?)
    fun onTyping(typing: TypingDto)
    fun onMessageReaction(event: MessageReactionEventDto, cursor: String?)
    fun onMessageDeleted(event: MessageDeletedEventDto, cursor: String?)
    fun onMessageEdited(message: MessageDto, cursor: String?)
    fun onMessageStatus(event: MessageStatusEventDto, cursor: String?)
    fun onConnectedEvent(cursor: String?)
    fun onError(error: ChatError)
    fun onOpen()
    fun onClosed(code: Int, reason: String)
}
