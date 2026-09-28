package com.webitel.chat.sdk.internal.transport.dto

import com.webitel.chat.sdk.MessageSearchCursor


internal data class MessageSearchResultDto(
    val items: List<MessageDto>,
    val newerCursor: MessageSearchCursor?,
    val olderCursor: MessageSearchCursor?
)
