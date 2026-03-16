package com.webitel.chat.sdk


/**
 * Represents a chat contact.
 */
data class Contact(
    /** Unique identifier of the contact. */
    val id: ContactId,

    /** Display name of the contact. */
    val name: String,

    /**
     * Source of the contact (e.g. telegram, facebook, webitel, custom).
     * Corresponds to the `iss` field configured on the server.
     */
    val source: String,

    /**
     * Indicates whether the contact represents a bot.
     */
    val isBot: Boolean
)


/**
 * Unique identity of a contact.
 */
data class ContactId(
    /** Subject identifier of the contact. */
    val sub: String,

    /** Issuer of the contact identity. */
    val iss: String
)