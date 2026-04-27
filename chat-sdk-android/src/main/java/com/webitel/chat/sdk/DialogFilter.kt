package com.webitel.chat.sdk

/**
 * Dialog filter.
 */
data class DialogFilter(

    /**
     * Full-text search query (up to 256 characters).
     *
     * Performs a case-insensitive partial match (`ilike`) against:
     * - `subject` for group/channel dialogs
     * - `title` for direct dialogs
     *
     * Matches both exact values and substrings anywhere in the text.
     */
    val query: String? = null,

    /** Filter by specific dialog IDs. */
    val ids: List<String>? = null,

    /**
     * Filter by dialog types (e.g. direct, group, channel).
     * `UNKNOWN` is ignored and not sent to the server.
     * If no valid types are provided, the filter is not applied.
     */
    val types: List<DialogType>? = null
)