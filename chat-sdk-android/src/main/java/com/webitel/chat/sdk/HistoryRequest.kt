package com.webitel.chat.sdk


/**
 * Request parameters used to load a portion of dialog history.
 */
data class HistoryRequest(
    /** Maximum number of items to return. */
    val limit: Int = 50,

    /** Optional cursor used to continue history navigation. */
    val cursor: HistoryCursor? = null,
)

/**
 * Cursor pointing to a specific message in history.
 * Used to load messages before or after the referenced item.
 */
data class HistoryCursor(
    /** Identifier of the reference message. */
    val messageId: String,

    /** Timestamp of the reference message. */
    val createdAt: Long,

    /** Direction in which history should be loaded. */
    val direction: MoveDirection = MoveDirection.BEFORE
)

/**
 * Direction used when navigating message history.
 */
enum class MoveDirection {
    /** Load messages that were sent before the cursor. */
    BEFORE,

    /** Load messages that were sent after the cursor. */
    AFTER
}