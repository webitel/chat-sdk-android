package com.webitel.chat.sdk


/**
 * Aggregated reaction of a given emoji on a message.
 *
 * One entry exists per distinct emoji used on the message;
 * counters and reactor list are computed by the server.
 */
data class MessageReaction(

    /** The emoji this reaction represents. */
    val emoji: String,

    /** Number of participants who reacted with this emoji. */
    val count: Int,

    /** Whether the current user is among the reactors. */
    val reactedByMe: Boolean,

    /** Identifiers of participants who reacted with this emoji. */
    val reactorIds: List<String>,

    /** Timestamp of the most recent reaction with this emoji, in milliseconds since epoch. */
    val lastReactedAt: Long?
)
