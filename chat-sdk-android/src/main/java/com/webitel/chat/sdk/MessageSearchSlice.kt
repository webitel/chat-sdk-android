package com.webitel.chat.sdk


/**
 * Represents a portion of message search results with cursors for pagination.
 */
data class MessageSearchSlice(
    /** Messages matching the search request. */
    val items: List<Message>,

    /** Cursor used to load newer search results. */
    val newerCursor: MessageSearchCursor?,

    /** Cursor used to load older search results. */
    val olderCursor: MessageSearchCursor?
) {

    /** Indicates whether the slice contains no messages. */
    val isEmpty: Boolean
        get() = items.isEmpty()
}
