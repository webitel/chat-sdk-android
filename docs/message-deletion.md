# Message Deletion

Messages can be deleted in batch, by identifier.

The underlying endpoint has no notion of a dialog — `ids` are not restricted to the dialog `deleteMessages` was called on, and any accessible message id can be passed regardless of which `Dialog` instance you call this on, or whether you call it via `chatClient` instead. This mirrors `setReaction`/`removeReaction`, which are available on both `Dialog` and `ChatClient` the same way. Scoping and authorization are enforced server-side; unauthorized or non-existent ids are reported back via `skipped` instead of failing the whole call.


## Deleting Messages

Messages are deleted via a dialog instance or via the client:

```kotlin
/**
 * Deletes messages by identifier.
 *
 * This is a batch operation — it is not scoped to a particular dialog,
 * so ids from different dialogs may be passed together. Scoping and
 * authorization are enforced server-side: unauthorized or non-existent
 * ids are reported back via [MessageDeletionResult.skipped] instead of
 * failing the whole call.
 *
 * A realtime [MessageEvent.Deleted] is dispatched for each deleted
 * message, including deletions made by another participant.
 *
 * @param ids Identifiers of the messages to delete
 * @param onComplete Callback invoked with the result of the operation
 */
fun deleteMessages(
    ids: List<String>,
    onComplete: (Result<MessageDeletionResult>) -> Unit
)
```

```kotlin
dialog.deleteMessages(ids = listOf(message.id)) { result ->
    result
        .onSuccess { deletion -> }
        .onFailure { error -> }
}

// Or via the client, without a dialog instance:
chatClient.deleteMessages(ids = listOf(message.id)) { result -> }
```


## Result

```kotlin
/** Result of a [Dialog.deleteMessages]/[ChatClient.deleteMessages] call. */
data class MessageDeletionResult(

    /** Timestamp of the deletion, in milliseconds since epoch. */
    val deletedAt: Long?,

    /** Identifiers of the messages that were actually deleted. */
    val deletedIds: List<String>,

    /** Requested identifiers that were not deleted, with the reason why. */
    val skipped: List<SkippedMessage>,
)
```

```kotlin
/** A message id that was not deleted, with the reason why. */
data class SkippedMessage(
    val id: String,
    val reason: SkippedMessageReason,
)
```

```kotlin
/** Reason a message id was not deleted, reported in [SkippedMessage.reason]. */
enum class SkippedMessageReason(val value: String) {
    NOT_FOUND("REASON_NOT_FOUND"),
    NOT_AUTHOR("REASON_NOT_AUTHOR"),
    ALREADY_DELETED("REASON_ALREADY_DELETED"),
    CHAT_CLOSED("REASON_CHAT_CLOSED"),
    NOT_ALLOWED("REASON_NOT_ALLOWED"),

    /** Unrecognized value received from the server. */
    UNKNOWN("")
}
```

`reason` falls back to `UNKNOWN` for values the SDK does not yet recognize, so switching over it should always handle the `UNKNOWN` case (or be non-exhaustive).


## Receiving Deletion Updates

A realtime `MessageEvent.Deleted` is dispatched when a message is deleted — including deletions made by another participant — carrying who deleted it and when:

```kotlin
data class Deleted(
    override val dialogId: String,
    val deletion: MessageDeletion,
) : MessageEvent()

data class MessageDeletion(
    val messageId: String,
    val deletedBy: Participant,
    val deletedAt: Long,
)
```

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.Deleted -> {
            // remove event.deletion.messageId from the local message list,
            // or replace it with a "Message deleted" placeholder
        }
        else -> Unit
    }
}
```

See [Events](events.md) for how to register a listener.

The dialog's cached `lastMessage` is cleared automatically if it was the deleted message. The SDK does not keep a full local history — for other messages (e.g. items already loaded via `getHistory`), the client is responsible for applying `MessageEvent.Deleted` to its own local state.
