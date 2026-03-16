package com.webitel.chat.sdk


/**
 * Request parameters used to load a page of contacts.
 */
data class ContactRequest(

    /** Page number to load. */
    val page: Int = 1,

    /** Number of contacts per page. */
    val size: Int = 50
)