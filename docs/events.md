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
        val messageId: String,
        val newText: String,
    ) : MessageEvent()

    /** Emitted when a message is deleted. */
    data class Deleted(
        override val dialogId: String,
        val messageId: String,
    ) : MessageEvent()
}
```

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

### State events

```kotlin
sealed class StateEvent : ChatEvent {
    /** Emitted when a participant is typing. */
    data class Typing(
        override val dialogId: String,
        val userId: String,
    ) : StateEvent()

    /** Emitted when a message is marked as read by a participant. */
    data class Read(
        override val dialogId: String,
        val messageId: String,
        val contactId: String,
    ) : StateEvent()
}
```

## Handling events

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is MessageEvent.Received -> {
            event.message
        }
        is MessageEvent.Edited -> {
            // update message content
        }
        is MessageEvent.Deleted -> {
            // remove or mark as deleted
        }
        is StateEvent.Typing -> {
            // show typing indicator
        }
        is StateEvent.Read -> {
            // update read status
        }
        is DialogEvent.Created -> {
            event.dialog
        }
    }
}
```