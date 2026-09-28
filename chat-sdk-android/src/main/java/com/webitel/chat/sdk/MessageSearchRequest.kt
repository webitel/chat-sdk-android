package com.webitel.chat.sdk


/**
 * Request parameters used to search messages by text query.
 */
data class MessageSearchRequest(
    /** Search text, matched as a case-insensitive substring. Expected to be 1...256 characters, server-validated. */
    val query: String,

    /** Optional filter by message sender. Expected to be up to 50 identifiers, server-validated. */
    val senderIds: Set<String> = emptySet(),

    /** Optional filter by message content type. Expected to be up to 8 values, server-validated. */
    val contentTypes: Set<MessageSearchContentType> = emptySet(),

    /** Maximum number of items to return. Clamped to the 1...100 range. */
    val limit: Int = 20,

    /** Optional cursor used to continue search navigation. */
    val cursor: MessageSearchCursor? = null,
)

/**
 * Content type of a message, as used to filter search results.
 */
enum class MessageSearchContentType(internal val code: Int) {
    TEXT(1),
    DOCUMENT(2),
    IMAGE(3),
    SYSTEM(4),
    INTERACTIVE(5),
    LOCATION(6),
    CONTACT(7)
}
