# Message Reply

Users can reply to a specific message, attaching a lightweight reference
to the original message that is included in the new message.


## Model

```kotlin
data class MessageReply(

    /** Identifier of the original message being replied to. */
    val messageId: String,

    /** Sender of the original message. */
    val from: Participant,

    /** Creation timestamp of the original message, in milliseconds since epoch. */
    val createdAt: Long,

    /**
     * Whether the original message has since been deleted.
     *
     * When `true`, [content] still carries a text preview (if any),
     * but attachment/location metadata is stripped by the server.
     */
    val isDeleted: Boolean,

    /** Preview of the original message content. */
    val content: MessageReplyContent
)
```

```kotlin
sealed class MessageReplyContent {
    data class Text(val text: String?) : MessageReplyContent()
    data class Image(val caption: String?, val mimeType: String?) : MessageReplyContent()
    data class Document(val name: String?, val mimeType: String?, val caption: String?) : MessageReplyContent()
    data class Location(val name: String?, val address: String?) : MessageReplyContent()
    data class Contact(val displayValue: String?) : MessageReplyContent()
    data class System(val text: String) : MessageReplyContent()
    data class Interactive(val text: String?) : MessageReplyContent()
    data class Unsupported(val type: String, val text: String?) : MessageReplyContent()
}
```

`Message.reply` holds a preview of the message being replied to, or `null` if the message is not a reply.


## Sending a Reply

Reply is set via `MessageOptions.replyToMessageId` — no new send method is needed,
the existing `dialog.sendMessage(...)` / `chatClient.sendMessage(...)` are used as-is.

```kotlin
val options = MessageOptions(
    content = SendContent.Text("Sounds good!"),
    replyToMessageId = originalMessage.id
)

dialog.sendMessage(options) { result ->
    result
        .onSuccess { messageId -> }
        .onFailure { error -> }
}
```


## Receiving a Reply

`Message.reply` is populated automatically for every message that carries a
reply reference — in history, in realtime `MessageEvent.Received`, and in
`MessageEvent.Edited`.

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.Received -> {
            event.message.reply?.let { reply ->
                // render a reply-quote above the message body
            }
        }
        else -> Unit
    }
}
```

There is no dedicated realtime event for reply changes — a reply reference
is set once when the message is created and never changes afterwards.

See [Events](events.md) for how to register a listener.
