package com.webitel.chat.sdk


/**
 * Type of dialog.
 */
enum class DialogType {
    /** One-to-one dialog between two participants. */
    DIRECT,

    /** Dialog with multiple participants. */
    GROUP,

    /** Channel-style dialog (broadcast or topic based communication). */
    CHANNEL,

    /** Unknown or unsupported dialog type. */
    UNKNOWN;

    companion object {

        /**
         * Converts a raw string value (usually from API)
         * into a [DialogType]. Returns [UNKNOWN] if no match found.
         */
        fun from(value: String): DialogType {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}