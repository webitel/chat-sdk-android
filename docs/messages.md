# Messages

## Sending Messages

Messages can be sent in two ways:
- via a dialog instance: dialog.sendMessage(...)  
- via the client: chatClient.sendMessage(...) (with an explicit target)  

After a successful send, the server returns the messageId of the created message.

If realtime is active, the same message will also be delivered via events.


## Tracking outgoing messages (sendId)
To correctly update UI state (e.g. “pending → delivered”), you can provide a sendId — a client-generated unique identifier.

This sendId will be included in the message received via realtime events, allowing you to match the local message with the server-confirmed one.


## Example
```kotlin
val target: MessageTarget = MessageTarget.Dialog("dialogId")
// or:
// val target: MessageTarget = MessageTarget.Contact(ContactId(contactSUB, contactISS))

val sendId = UUID.randomUUID().toString()

val options = MessageOptions(
    content = SendContent.Text("Hello"),
    sendId = sendId
)

chatClient.sendMessage(target, options) { result ->
    result
        .onSuccess { messageId -> }
        .onFailure { error -> }
}
```


## Message Content

The SDK uses two different content models depending on the direction of the message:

- SendContent — used when sending messages  
- MessageContent — used for messages received from the server  

This separation exists because the server may enrich or transform messages before delivering them.


### Sending content (SendContent)

SendContent defines what the client is allowed to send.

Supported types:

- Text  
- Attachments  
- Composite (text + attachments)  
- Contact  
- Location  

Example:
```kotlin
val content = SendContent.Composite(
    text = "See attached file",
    attachments = listOf(file)
)
```

### Received content (MessageContent)

MessageContent represents the final message as delivered by the server.

It may include additional data not present in the original request.

Supported types:

- Text  
- Attachments  
- Composite  
- Contact  
- Location  
- KeyboardOnly  
- System  

Example:

```kotlin
when (val content = message.content) {
    is MessageContent.Text -> { /* ... */ }
    is MessageContent.Composite -> { /* ... */ }
    is MessageContent.System -> { /* ... */ }
}
```

#### File Metadata in Messages

Received messages may contain file attachments.

Example:

```kotlin
when (val content = message.content) {
    is MessageContent.Attachments -> {
        content.attachments.forEach { attachment ->
            
        }
    }

    is MessageContent.Composite -> {
        content.attachments.forEach { attachment ->

        }
    }
}
```

Attachments contain metadata only.

File content must be downloaded explicitly using the attachment file identifier.

## Message model

```kotlin
data class Message(

    /** Unique message identifier */
    val id: String,

    /** Dialog identifier */
    val dialogId: String,

    /** Message creation timestamp (milliseconds since epoch) */
    val createdAt: Long,

    /** Last edit timestamp (null if never edited) */
    val editedAt: Long?,

    /** Sender of the message */
    val from: Participant,

    /**
     * Message content.
     *
     * See [MessageContent] for all supported content types and combinations.
     */
    val content: MessageContent,

    /** Client-generated identifier for message tracking */
    val sendId: String? = null,

    /** Indicates whether the message is outgoing */
    val isOutgoing: Boolean,

    /**
     * Position of the message within its dialog (null if not provided).
     * Compare with read/delivery horizons, see [Events](events.md#receipt-events).
     */
    val sequence: Long? = null,

    /** Members this message failed to be delivered to (empty if no failures are known) */
    val deliveryExceptions: List<DeliveryException> = emptyList()
)
```

See [Delivery Failures](#delivery-failures) for `deliveryExceptions`.


## Delivery Failures

A message may fail to reach some dialog members — e.g. an external channel rejects it (Telegram: `403 Forbidden: bot was blocked by the user`). Each failure is described per member:

```kotlin
data class DeliveryException(
    val messageId: String,
    /** Member the message was not delivered to */
    val member: Participant,
    val error: DeliveryError
)

data class DeliveryError(
    /** e.g. "403" */
    val code: String,
    val message: String?
)
```

Failures are part of the message itself (`failures` on the server) and arrive through every channel:

| Source | How the client applies it |
|---|---|
| History (`getHistory`) | `Message.deliveryExceptions` is already filled |
| Sync after reconnect (`DialogEvent.Synchronized`) | `changes.messages` carry the whole updated message — upsert by `id` |
| Message edit (`MessageEvent.Edited`) | `event.message` carries the current failures |
| Realtime (`ReceiptEvent.DeliveryFailed`) | Find the message by `exception.messageId` and add / replace the failure for `exception.member` |

```kotlin
is ReceiptEvent.DeliveryFailed -> {
    val exception = event.exception
    updateMessage(event.dialogId, exception.messageId) { message ->
        message.copy(
            deliveryExceptions = message.deliveryExceptions
                .filterNot { it.member.id == exception.member.id } + exception
        )
    }
}
```

Dialogs do not keep a separate list of failures; the SDK only patches the cached `Dialog.lastMessage` when it is the failed message.


## Message History

Message history is retrieved via a dialog instance:
```kotlin
val request = HistoryRequest()

dialog.getHistory(request) { result ->
    result
        .onSuccess { historySlice -> }
        .onFailure { error -> }
}
```


### Request parameters
HistoryRequest supports:
- limit — number of messages to load  
- cursor — pagination position (HistoryCursor)  

```kotlin
data class HistoryCursor(

    /** Identifier of the reference message */
    val messageId: String,

    /** Direction of loading */
    val direction: MoveDirection = MoveDirection.OLDER
)
```

`MoveDirection` values:

- `OLDER` — load messages older than the cursor (default)  
- `NEWER` — load messages newer than the cursor  
- `AROUND` — load messages around the cursor (both older and newer)  


### Result

```kotlin
data class HistorySlice<T>(
    /** Items returned in this slice of history. */
    val items: List<T>,

    /** Cursor used to load messages newer than this slice. */
    val newerCursor: HistoryCursor?,

    /** Cursor used to load messages older than this slice. */
    val olderCursor: HistoryCursor?
)
```

- `olderCursor` — used to load older messages  
- `newerCursor` — used to load newer messages  


### Working with cursors

Cursors can also be created manually. This is useful, for example, after reconnect:
- take the last known message and use id   
- set direction to MoveDirection.NEWER  

This allows checking whether new messages after the connection was restored.


### Jumping to a Message

To open history at a specific message (for example, when the user taps a message search result), use the `AROUND` direction. The dialog can be resolved via `message.dialogId`.

```kotlin
val request = HistoryRequest(
    limit = 50,
    cursor = HistoryCursor(messageId = message.id, direction = MoveDirection.AROUND)
)

dialog.getHistory(request) { result ->
    result.onSuccess { slice ->
        // Continue pagination from this point using slice.olderCursor / slice.newerCursor
    }
}
```

See [Message Search](message-search.md) for searching messages by text.


## Marking Messages as Read

Messages are marked as read via a dialog instance, either by sequence or by message id:
- `dialog.markAsRead(sequence)` — using `Message.sequence`
- `dialog.markAsRead(messageId)` — using `Message.id`

All messages in the dialog up to and including the given message are considered read. Both variants behave the same on the server; `Message.sequence` is optional, so fall back to `messageId` when it is `null`.

```kotlin
/** Marks messages in this dialog as read up to the given sequence. */
fun markAsRead(
    sequence: Long,
    onComplete: (Result<Unit>) -> Unit
)

/** Marks messages in this dialog as read up to the given message. */
fun markAsRead(
    messageId: String,
    onComplete: (Result<Unit>) -> Unit
)
```

```kotlin
val onComplete: (Result<Unit>) -> Unit = { result ->
    result
        .onSuccess { /* marked as read */ }
        .onFailure { error -> println("Failed to mark as read: $error") }
}

val sequence = message.sequence

if (sequence != null) {
    dialog.markAsRead(sequence, onComplete)
} else {
    dialog.markAsRead(message.id, onComplete)
}
```

Unlike `editMessage`/`deleteMessages`, this operation is scoped to a dialog (`POST /api/v1/threads/{dialogId}/read`), so it is available only on `Dialog`, not on `ChatClient` — the same as `sendTyping`.

Other participants receive a `ReceiptEvent.Read`, see [Events](events.md#receipt-events).
