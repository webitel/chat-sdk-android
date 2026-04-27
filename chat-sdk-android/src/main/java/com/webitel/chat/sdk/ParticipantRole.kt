package com.webitel.chat.sdk

/**
 * Participant role.
 */
enum class ParticipantRole(val value: String) {

    /** The highest role in the chat. */
    OWNER("ROLE_OWNER"),

    /** Administrator role. */
    ADMIN("ROLE_ADMIN"),

    /** Supervisor role. */
    SUPERVISOR("ROLE_SUPERVISOR"),

    /** Regular participant of the chat. */
    MEMBER("ROLE_MEMBER"),

    /** Unspecified role */
    UNSPECIFIED("ROLE_UNSPECIFIED");

    companion object {
        fun fromValue(value: String): ParticipantRole {
            return entries.find { it.value == value } ?: UNSPECIFIED
        }
    }
}