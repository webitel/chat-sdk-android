package com.webitel.chat.sdk


/**
 * Represents a portion of history items with cursors for pagination.
 */
data class HistorySlice<T>(
    /** Items returned in this slice of history. */
    val items: List<T>,

    /** Cursor used to load messages newer than this slice. */
    val newerCursor: HistoryCursor?,

    /** Cursor used to load messages older than this slice. */
    val olderCursor: HistoryCursor?
)