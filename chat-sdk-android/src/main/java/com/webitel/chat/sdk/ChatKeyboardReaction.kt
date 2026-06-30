package com.webitel.chat.sdk


data class ChatKeyboardReaction(
    /** Identifier of the selected button. */
    val buttonCode: String,

    /** Callback data associated with the button. */
    val callbackData: String,

    /** Time when the button was pressed. */
    val reactedAt: Long,

    /** Participant who pressed the button. */
    val reactedBy: Participant
)