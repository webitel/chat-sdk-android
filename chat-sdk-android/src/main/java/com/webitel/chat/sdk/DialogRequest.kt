package com.webitel.chat.sdk


/**
 * Request parameters used to load a page of dialogs.
 *
 * Supports pagination and optional filtering:
 * - `page` and `size` control pagination
 * - `filter` allows narrowing results by query, ids, or dialog types
 *
 * If `filter` is not provided, all dialogs are returned.
 */
data class DialogRequest(

    /** Page number to load. */
    val page: Int = 1,

    /** Number of dialogs per page. */
    val size: Int = 50,

    /**
     * Optional filter applied to dialogs.
     * See [DialogFilter] for available options.
     */
    val filter: DialogFilter? = null
)