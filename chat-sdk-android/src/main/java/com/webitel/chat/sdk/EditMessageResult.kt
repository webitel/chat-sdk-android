package com.webitel.chat.sdk


/** Result of a [Dialog.editMessage]/[ChatClient.editMessage] call. */
data class EditMessageResult(
    val messageId: String,
    val editedAt: Long,
)
