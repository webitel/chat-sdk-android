package com.webitel.chat.sdk

/**
 * Represents an action performed on a message.
 *
 * Actions are typically triggered by user interactions in the UI
 * and are sent to the server for processing.
 */
sealed interface MessageAction {

    /**
     * Action triggered by clicking a button.
     *
     * @param id Identifier of the button
     * @param data Additional payload associated with the button
     */
    data class ButtonClick(
        val id: String,
        val data: String
    ) : MessageAction
}