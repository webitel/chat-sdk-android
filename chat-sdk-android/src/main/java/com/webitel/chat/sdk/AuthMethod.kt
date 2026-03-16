package com.webitel.chat.sdk


/**
 * Authentication method used by the chat client.
 */
sealed class AuthMethod {

    /**
     * Authentication using an access token.
     * The token is provided via a lambda to allow dynamic refresh.
     */
    data class Token(
        val tokenProvider: () -> String
    ) : AuthMethod() {

        override fun toString() = "AuthMethod.Token"
    }

    /**
     * Authentication using a contact identity.
     */
    data class Contact(
        val identity: ContactIdentity
    ) : AuthMethod() {

        /** Hides identity details in logs. */
        override fun toString() = "AuthMethod.Contact"
    }
}