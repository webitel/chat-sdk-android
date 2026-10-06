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

Listeners are called on a single background SDK thread (`chat-sdk-sync`), never on the main thread, in the order events arrive. Switch threads before touching UI.

Do not block inside `onEvent`: the same thread processes all realtime events and the synchronization after a reconnect, so a slow listener delays every following event.


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

See [Message Reply](message-reply.md) for details on `Message.reply`.

See [Message Forward](message-forward.md) for details on `Message.forwardOrigin`.

### Dialog events

```kotlin
sealed class DialogEvent : ChatEvent {

    /** Emitted when a new dialog is created. */
    data class Created(
        override val dialogId: String,
        val dialog: Dialog
    ) : DialogEvent()

    /** Emitted with changes missed while the realtime connection was unavailable. */
    data class Synchronized(
        override val dialogId: String,
        val changes: DialogSyncChanges
    ) : DialogEvent()
}
```

`Synchronized` is dispatched after a reconnect for every dialog that changed while the realtime connection was unavailable. See [Synchronization](#synchronization).

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

### Receipt events

```kotlin
sealed class ReceiptEvent : ChatEvent {

    data class Delivered(
        override val dialogId: String,
        val member: Participant,
        val upToSequence: Long
    ) : ReceiptEvent()

    data class Read(
        override val dialogId: String,
        val member: Participant,
        val upToSequence: Long
    ) : ReceiptEvent()

    data class DeliveryFailed(
        override val dialogId: String,
        val exception: DeliveryException
    ) : ReceiptEvent()
}
```

Receipts are cumulative horizons: `Read(upToSequence = 108)` means every message with `Message.sequence <= 108` is read by `member`.

The SDK first advances `Dialog.participantStates`, then dispatches the event — only when the horizon actually moved forward. Duplicate or stale receipts are dropped, so a horizon never goes back. `Read` and `Delivered` are tracked independently.

```kotlin
data class ParticipantState(
    val member: Participant,
    val deliveredUpToSequence: Long,
    val readUpToSequence: Long
)
```

The same `participantStates` are loaded with the dialog and refreshed after a reconnect (see [Synchronization](#synchronization)), so the dialog always holds the current state regardless of the source:

```kotlin
is ReceiptEvent.Read -> {
    markRead(event.dialogId, event.member.id, upTo = event.upToSequence)
}
```

`DeliveryFailed` is reserved for future use — not yet emitted by the server.

See [Marking Messages as Read](messages.md#marking-messages-as-read) for sending read receipts.


## Synchronization

When the realtime connection drops (network loss, backgrounding, backoff), the SDK remembers the position of the last processed event. On every **re**connect it fetches the changes missed since that position and dispatches them as `DialogEvent.Synchronized` — one event per changed dialog. If the server reports on reconnect that nothing changed since that position, no request is made and no events are dispatched. Realtime events received while the sync is in progress are held back and delivered afterwards, without duplicates. While the sync runs, new messages and other live events are therefore delayed until the recovered `Synchronized` events are dispatched (typing events are not held back).

```kotlin
data class DialogSyncChanges(
    val unreadCount: Int,
    /** Changed messages, sorted by `sequence`. Upsert by `id`. */
    val messages: List<Message>,
    /** Messages to remove from local state. */
    val deletedMessageIds: List<String>,
    /**
     * Current delivery/read horizons; compare with `Message.sequence`.
     * Same merged values as `Dialog.participantStates`.
     */
    val participantStates: List<ParticipantState>,
    val deliveryExceptions: List<DeliveryException>,
    val memberChanges: List<MemberChange>,
    /** The current user has left the dialog. */
    val hasLeft: Boolean
)
```

The dialog's cached `lastMessage`, `members` and `participantStates` are updated automatically. Recovered horizons are merged with the ones received in realtime and never move back.

Dialogs created while offline are announced first, so they are handled by the same code as realtime `Created`:

```
DialogEvent.Created(dialogId, dialog)
DialogEvent.Synchronized(dialogId, changes)
```

If a `Synchronized` still refers to a dialog unknown to the client, fetch it by `dialogId`:

```kotlin
is DialogEvent.Synchronized -> {
    if (dialogs[event.dialogId] == null) {
        val request = DialogRequest(filter = DialogFilter(ids = listOf(event.dialogId)))

        chatClient.getDialogs(request) { result ->
            result.onSuccess { page ->
                page.items.firstOrNull()?.let { dialogs[event.dialogId] = it }
            }
        }
    }
    apply(event.changes, event.dialogId)
}
```

### Full resync

If too many changes were missed (the server requests a resync, or the changes span more than 50 pages), or fetching them fails twice (one retry), the SDK notifies client listeners instead. Realtime events held back during the sync are still delivered. This is not a `ChatEvent`, because it is not bound to a dialog:

```kotlin
interface ChatClientListener {
    /** Default implementation is empty. */
    fun onResyncRequired() {}
}
```

```kotlin
class ChatStore(chatClient: ChatClient) : ChatClientListener {
    init {
        chatClient.addClientListener(this)
    }

    override fun onResyncRequired() {
        reloadAll() // dialog list and any opened message histories
    }
}
```

Remove the listener with `removeClientListener(listener)`.

### Cold start

The position is kept in memory only and starts with the first realtime connection. Call `connect()` **before** the initial `getDialogs` / `getHistory`, or reload them after the first `Connected` state — otherwise changes made between the initial load and the connection are not recovered. The position is reset by `endSession()`.

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
        is DialogEvent.Synchronized -> {
            // apply event.changes to the dialog
        }
        is ReceiptEvent.Read -> {
            // update read state up to event.upToSequence
        }
        is ReceiptEvent.Delivered -> {
            // update delivered state up to event.upToSequence
        }
        is ReceiptEvent.DeliveryFailed -> {
            // reserved, not yet emitted
        }
    }
}
```