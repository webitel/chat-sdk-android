package com.webitel.chat.sdk

/**
 * Represents an interactive message keyboard.
 *
 * A keyboard provides a set of buttons that a user can interact with.
 * It can be rendered either as:
 * - a grid of inline buttons, or
 * - a list-style menu grouped into sections.
 */
sealed class ChatKeyboard {
    /**
     * Grid-style keyboard (similar to inline buttons).
     *
     * Buttons are arranged into horizontal rows.
     */
    data class Buttons(
        val rows: List<ChatKeyboardRow>
    ) : ChatKeyboard()

    /**
     * List-style keyboard with a main trigger button.
     *
     * When the user taps the main button, a structured list
     * of sections with buttons is displayed.
     */
    data class ListMenu(
        val title: String,
        val sections: List<ChatKeyboardSection>
    ) : ChatKeyboard()
}


/**
 * Represents a section inside a list-style keyboard.
 *
 * Each section has a header and a list of buttons.
 */
data class ChatKeyboardSection(
    /** Section title shown to the user */
    val title: String,

    /** Buttons belonging to this section */
    val buttons: List<ChatKeyboardButton>
)


/**
 * Represents a horizontal row of buttons.
 */
data class ChatKeyboardRow(
    /** Buttons displayed in a single row */
    val buttons: List<ChatKeyboardButton>
)


/**
 * Represents a single interactive button in the keyboard.
 *
 * Each button has a unique identifier and a specific action.
 */
data class ChatKeyboardButton(
    /**
     * Unique identifier of the button.
     *
     * Can be used to prevent duplicate actions or track state.
     */
    val id: String,

    /** Text displayed on the button */
    val label: String,

    /** Action that will be triggered on click */
    val action: ChatButtonAction,

    /**
     * Optional metadata for UI customization.
     *
     * Example use cases:
     * - color = "primary"
     * - size = "large"
     */
    val metadata: Map<String, Any>? = null
)


/**
 * Defines the behavior of a keyboard button.
 */
sealed class ChatButtonAction {
    /**
     * Opens an external URL.
     *
     * The client is responsible for handling navigation.
     */
    data class OpenUrl(
        val url: String
    ) : ChatButtonAction()

    /**
     * Sends a callback event to the backend.
     *
     * Typically used for bot interactions or server-side logic.
     */
    data class SendCallback(
        val data: String
    ) : ChatButtonAction()

    /**
     * Requests data from the client device.
     *
     * Example:
     * - "location"
     * - "contact"
     */
    data class RequestData(
        val type: String
    ) : ChatButtonAction()
}


val ChatKeyboardButton.isUrl get() = action is ChatButtonAction.OpenUrl
val ChatKeyboardButton.isCallback get() = action is ChatButtonAction.SendCallback
val ChatKeyboardButton.isRequest get() = action is ChatButtonAction.RequestData