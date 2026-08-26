package com.webitel.chat.sdk


/**
 * Result of a [Dialog.setReaction]/[ChatClient.setReaction] call.
 *
 * Describes only the outcome for the current user's own reaction.
 * The full aggregated reaction list for the message is delivered
 * separately via [MessageEvent.ReactionsChanged].
 */
data class ReactionResult(
    val action: ReactionAction,
    val emoji: String?,
    val reactedAt: Long?
)


/**
 * Convenience for clearing the current user's reaction on a message.
 *
 * Equivalent to calling [Dialog.setReaction] with an empty emoji.
 */
fun Dialog.removeReaction(
    messageId: String,
    onComplete: (Result<ReactionResult>) -> Unit
) = setReaction(messageId = messageId, emoji = "", sendId = null, onComplete = onComplete)


/**
 * Convenience for clearing the current user's reaction on a message.
 *
 * Equivalent to calling [ChatClient.setReaction] with an empty emoji.
 */
fun ChatClient.removeReaction(
    messageId: String,
    onComplete: (Result<ReactionResult>) -> Unit
) = setReaction(messageId = messageId, emoji = "", sendId = null, onComplete = onComplete)
