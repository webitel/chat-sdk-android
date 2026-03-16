package com.webitel.chat.sdk


/**
 * Represents a portion of history items with cursors for pagination.
 */
data class HistorySlice<T>(
    /** Items returned in this slice of history. */
    val items: List<T>,

    /** Cursor to load items that come before this slice. */
    val beforeCursor: HistoryCursor?,

    /** Cursor to load items that come after this slice. */
    val afterCursor: HistoryCursor?
)