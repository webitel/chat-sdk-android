# Events

Realtime Event Handling

To receive messages and other realtime updates, the SDK uses an event listener that subscribes to WebSocket events.

A listener can be registered:  
- globally — to receive events from all dialogs  
- per dialog — to receive events only for a specific dialog  

Global listener:
```kotlin
chatClient.addEventListener(this)
```

Dialog-specific listener:
```kotlin
dialog.addListener(this)
```
Receives only events related to the specific dialog.


## Interface

```kotlin
interface ChatEventListener {
    /** Called when a new [ChatEvent] is emitted. */
    fun onEvent(event: ChatEvent)
}
```


## Event model
All events are represented by the ChatEvent interface:
```kotlin
sealed interface ChatEvent {
    val dialogId: String
}
```


## Event types

### Message events

```kotlin
sealed class MessageEvent : ChatEvent {
    /** Emitted when the SDK receives a message from the server. */
    data class Received(
        override val dialogId: String,
        val message: Message,
    ) : MessageEvent()
    
    /** Emitted when an existing message is edited. */
    data class Edited(
        override val dialogId: String,
        val message: Message,
    ) : MessageEvent()

    /** Emitted when a message is deleted. */
    data class Deleted(
        override val dialogId: String,
        val deletion: MessageDeletion,
    ) : MessageEvent()

    /** Emitted when the aggregated reactions on a message changed. */
    data class ReactionsChanged(
        override val dialogId: String,
        val messageId: String,
        val reactions: List<MessageReaction>,
    ) : MessageEvent()
}
```

See [Message Editing](message-editing.md) for details on `Edited`.

See [Message Deletion](message-deletion.md) for details on `Deleted`.

See [Reactions](reactions.md) for details on `ReactionsChanged`.

### Dialog events

```kotlin
sealed class DialogEvent : ChatEvent {

    /** Emitted when a new dialog is created. */
    data class Created(
        override val dialogId: String,
        val dialog: Dialog
    ) : DialogEvent()
}
```

### Activity events

```kotlin
sealed class ActivityEvent : ChatEvent {
    /** Emitted when a participant is typing. */
    data class Typing(
        override val dialogId: String,
        val member: Participant,
        val previewText: String? = null,
        val timeoutMs: Long? = null,
    ) : ActivityEvent()
}
```

See [Typing Indicators](typing.md) for details on `Typing`.

## Handling events

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.Received -> {
            event.message
        }
        is MessageEvent.Edited -> {
            // replace the local message with event.message
        }
        is MessageEvent.Deleted -> {
            // remove or mark event.deletion.messageId as deleted
        }
        is MessageEvent.ReactionsChanged -> {
            // update reactions on the message
        }
        is ActivityEvent.Typing -> {
            // show typing indicator
        }
        is DialogEvent.Created -> {
            event.dialog
        }
    }
}
```