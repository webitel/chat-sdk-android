package com.webitel.chat.sdk


/**
 * Outcome of a [Dialog.setReaction]/[ChatClient.setReaction] call,
 * describing what happened to the current user's own reaction.
 */
enum class ReactionAction(val value: String) {
    /** The reaction was set (added or replaced a previous one). */
    SET("REACTION_ACTION_SET"),

    /** The reaction was removed. */
    REMOVED("REACTION_ACTION_REMOVED"),

    /** The request did not change the current reaction state. */
    UNCHANGED("REACTION_ACTION_UNCHANGED"),

    /** Unrecognized value received from the server. */
    UNKNOWN("");

    companion object {
        fun from(value: String): ReactionAction =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
