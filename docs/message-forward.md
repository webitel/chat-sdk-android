# Message Forward

A message can carry information about its original source when it was
forwarded — either from another Webitel chat, or from an external messenger.


## Model

```kotlin
data class MessageForwardOrigin(

    /** When the original message was sent, before it was forwarded, in milliseconds since epoch. */
    val originalSentAt: Long,

    /** Specific kind of origin and the data that comes with it. */
    val kind: MessageForwardOriginKind
) {
    /** Display name of the original sender, if known. */
    val senderDisplayName: String?
}
```

```kotlin
sealed class MessageForwardOriginKind {
    data class InternalUser(val senderId: String, val senderName: String?, val sourceMessageId: String) : MessageForwardOriginKind()
    data class ExternalUser(val senderName: String) : MessageForwardOriginKind()
    object ExternalHiddenUser : MessageForwardOriginKind()
    data class ExternalChat(val name: String?) : MessageForwardOriginKind()
    data class Unsupported(val rawKind: String, val senderName: String?) : MessageForwardOriginKind()
}
```

`Message.forwardOrigin` holds the origin of the message, or `null` if the
message was not forwarded.

- `InternalUser` — forwarded from another chat within Webitel; `sourceMessageId`
  references the original message.
- `ExternalUser` — forwarded from an external messenger, sender named.
- `ExternalHiddenUser` — forwarded from an external messenger where the sender
  chose to hide their identity.
- `ExternalChat` — forwarded from an external group or channel rather than a
  person.
- `Unsupported` — an origin kind not recognized by this version of the SDK;
  kept for forward compatibility instead of dropping the message.


## Receiving a Forwarded Message

`Message.forwardOrigin` is populated automatically for every message that
carries a forward reference — in history, in realtime `MessageEvent.Received`,
and in `MessageEvent.Edited`.

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.Received -> {
            event.message.forwardOrigin?.let { origin ->
                // render a "Forwarded from ..." label above the message body
            }
        }
        else -> Unit
    }
}
```

There is no dedicated realtime event for forward changes — a forward
reference is set once when the message is created and never changes
afterwards.

See [Events](events.md) for how to register a listener.


## Forwarding Messages

Messages are forwarded in batch via a dialog instance or via the client:
- via a dialog instance: `dialog.forwardMessages(ids)` — forwards into that dialog
- via the client: `chatClient.forwardMessages(ids, target)` — forwards to an explicit destination (dialog or contact)

```kotlin
fun forwardMessages(
    ids: List<String>,
    target: MessageTarget,
    sendId: String = UUID.randomUUID().toString(),
    onComplete: (Result<ForwardMessagesResult>) -> Unit
)
```

```kotlin
val target = MessageTarget.Dialog(id = "otherDialogId")

chatClient.forwardMessages(ids = listOf(messageId), target = target) { result ->
    result
        .onSuccess { forwardResult ->
            println("Forwarded: ${forwardResult.ids}")
            println("Skipped: ${forwardResult.skipped.map { "${it.id} (${it.reason})" }}")
        }
        .onFailure { error ->
            println("Failed to forward messages: $error")
        }
}
```

### Result

```kotlin
data class ForwardMessagesResult(

    /** Identifiers of the newly created forwarded messages. */
    val ids: List<String>,

    /** Requested identifiers that were not forwarded, with the reason why. */
    val skipped: List<SkippedMessage>,

    /** Identifier of the destination dialog the messages were forwarded into. */
    val threadId: String,
)
```

`skipped` lists messages the server did not forward along with the reason —
for example messages that no longer exist or that the current user is not
allowed to forward. `reason` falls back to `UNKNOWN` for values the SDK does
not yet recognize.

A general failure (e.g. the caller isn't authenticated) fails the whole
call via `onFailure` instead of appearing in `skipped` — `skipped` is only
for per-message issues, enumerated in `SkippedMessageReason`.

`ids` (the source ids being forwarded) are not restricted to the dialog
`forwardMessages` was called on — the underlying endpoint has no notion of a
source dialog, so any accessible message id can be passed regardless of
which `Dialog` instance you call this on, or whether you call it via
`chatClient` instead. This mirrors `deleteMessages`/`editMessage`.

`target` is filled the same way as for `sendMessage` — `MessageTarget.Dialog`
targets an existing dialog by id, `MessageTarget.Contact` targets a contact
directly (a dialog may be created automatically). `ForwardMessagesResult.threadId`
reports the destination dialog the messages ended up in, which is especially
useful when forwarding to a contact and a dialog was created automatically as
a result.
