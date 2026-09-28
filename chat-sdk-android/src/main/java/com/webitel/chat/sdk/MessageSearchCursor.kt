package com.webitel.chat.sdk


/**
 * Cursor pointing to a specific message in search results.
 */
data class MessageSearchCursor(
    /** Identifier of the reference message. */
    val messageId: String,

    /** Direction in which search results should be loaded. */
    val direction: SearchDirection = SearchDirection.OLDER
)

/**
 * Direction used when navigating search results.
 */
enum class SearchDirection {

    /** Load results older than the cursor. */
    OLDER,

    /** Load results newer than the cursor. */
    NEWER
}
