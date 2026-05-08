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
)
```


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