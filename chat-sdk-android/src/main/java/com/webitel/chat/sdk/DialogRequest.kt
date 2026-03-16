package com.webitel.chat.sdk


/**
 * Request parameters used to load a page of dialogs.
 */
data class DialogRequest(

    /** Page number to load. */
    val page: Int = 1,

    /** Number of dialogs per page. */
    val size: Int = 50
)