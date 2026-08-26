# Reactions

Users can react to messages with a single emoji.

Only one reaction per user is allowed per message — sending a new emoji replaces the previous one.


## Model

```kotlin
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
```

`Message.reactions` holds the current set of `MessageReaction` for a message — one entry per distinct emoji.


## Setting a Reaction

Reactions can be set via a dialog instance or via the client (with an explicit `messageId`).

```kotlin
/**
 * Sets or clears the current user's reaction on a message.
 *
 * Only one reaction per user is allowed per message — sending a new
 * emoji replaces the previous one. Passing an empty [emoji] clears
 * the current user's reaction (see [removeReaction] for a convenience
 * shortcut).
 *
 * The [onComplete] callback only reports the outcome for the current
 * user's own reaction. The full aggregated reaction list for the
 * message is delivered separately via [MessageEvent.ReactionsChanged].
 *
 * @param messageId Identifier of the message to react to
 * @param emoji Emoji to react with, or an empty string to clear the reaction
 * @param sendId Optional client-generated identifier for request tracking
 * @param onComplete Callback invoked with the result of the operation
 */
fun setReaction(
    messageId: String,
    emoji: String,
    sendId: String? = null,
    onComplete: (Result<ReactionResult>) -> Unit
)
```

`sendId` is optional — use it to correlate the request with realtime confirmation, similar to `sendId` for messages.

```kotlin
dialog.setReaction(messageId = message.id, emoji = "👍") { result ->
    result
        .onSuccess { reactionResult -> }
        .onFailure { error -> }
}

// Or via the client, without a dialog instance:
chatClient.setReaction(messageId = message.id, emoji = "👍") { result -> }
```


## Removing a Reaction

`removeReaction` clears the current user's reaction from a message.

```kotlin
fun Dialog.removeReaction(
    messageId: String,
    onComplete: (Result<ReactionResult>) -> Unit
)

fun ChatClient.removeReaction(
    messageId: String,
    onComplete: (Result<ReactionResult>) -> Unit
)
```

There is no separate removal endpoint — `removeReaction` is implemented as `setReaction(messageId, emoji = "", sendId = null, onComplete)` under the hood.


## Result

```kotlin
/** Outcome of a [Dialog.setReaction]/[ChatClient.setReaction] call. */
enum class ReactionAction(val value: String) {

    /** The reaction was set (added or replaced a previous one). */
    SET("REACTION_ACTION_SET"),

    /** The reaction was removed. */
    REMOVED("REACTION_ACTION_REMOVED"),

    /** The request did not change the current reaction state. */
    UNCHANGED("REACTION_ACTION_UNCHANGED"),

    /** Unrecognized value received from the server. */
    UNKNOWN("")
}
```

```kotlin
/** Result of setting or removing the current user's reaction on a message. */
data class ReactionResult(
    val action: ReactionAction,
    val emoji: String?,
    val reactedAt: Long?
)
```


## Receiving Reaction Updates

Reaction changes (from any user) are delivered via realtime events as `MessageEvent.ReactionsChanged`:

```kotlin
data class ReactionsChanged(
    override val dialogId: String,
    val messageId: String,
    val reactions: List<MessageReaction>,
) : MessageEvent()
```

The event carries the full current reaction list for the message, not a delta — replace the local list wholesale rather than merging it.

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.ReactionsChanged -> {
            // update reactions for event.messageId with event.reactions
        }
        else -> Unit
    }
}
```

See [Events](events.md) for how to register a listener.

The dialog's cached `lastMessage.reactions` is updated automatically only when this event's `messageId` matches `lastMessage.id`. For other messages (e.g. history items), the client is responsible for applying `MessageEvent.ReactionsChanged` to its own local state.
