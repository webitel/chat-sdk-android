# Message Editing

The text of an existing message can be edited after it was sent.


## Editing a Message

Messages are edited via a dialog instance or via the client (with an explicit `messageId`), the same way `setReaction`/`deleteMessages` are:

```kotlin
/**
 * Edits the text of an existing message.
 *
 * The [onComplete] callback only reports that the edit request was
 * accepted. The updated message content itself is delivered separately
 * via a realtime [MessageEvent.Edited], including edits made by another
 * participant.
 *
 * @param messageId Identifier of the message to edit
 * @param text New text content for the message
 * @param onComplete Callback invoked with the result of the operation
 */
fun editMessage(
    messageId: String,
    text: String,
    onComplete: (Result<EditMessageResult>) -> Unit
)
```

```kotlin
dialog.editMessage(messageId = message.id, text = "Updated text") { result ->
    result
        .onSuccess { editResult -> }
        .onFailure { error -> }
}

// Or via the client, without a dialog instance:
chatClient.editMessage(messageId = message.id, text = "Updated text") { result -> }
```


## Result

```kotlin
/** Result of a [Dialog.editMessage]/[ChatClient.editMessage] call. */
data class EditMessageResult(
    val messageId: String,
    val editedAt: Long,
)
```

`onComplete` only confirms the edit request itself — it does not carry the updated message. Use `MessageEvent.Edited` (below) to update local state.


## Receiving Edit Updates

A realtime `MessageEvent.Edited` is dispatched when a message is edited — including edits made by another participant — carrying the full updated message:

```kotlin
data class Edited(
    override val dialogId: String,
    val message: Message,
) : MessageEvent()
```

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.Edited -> {
            // replace the local message with event.message
        }
        else -> Unit
    }
}
```

See [Events](events.md) for how to register a listener.

The dialog's cached `lastMessage` is updated automatically when this event's `message.id` matches `lastMessage.id`, preserving `lastMessage.reactions` if the edit payload doesn't carry any. The SDK does not keep a full local history — for other messages (e.g. items already loaded via `getHistory`), the client is responsible for applying `MessageEvent.Edited` to its own local state.
