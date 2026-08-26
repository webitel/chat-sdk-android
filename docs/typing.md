# Typing Indicators

Notifies other dialog participants that the user is currently composing a message.


## Model

```kotlin
data class TypingRequest(

    /** Preview of the text currently being composed. */
    val previewText: String? = null,

    /** How long the typing indicator should remain active, in milliseconds. */
    val timeoutMs: Long? = null
)
```

Both fields are optional — `TypingRequest()` sends a plain typing indicator with no preview and no explicit timeout.


## Sending a Typing Indicator

Typing indicators are sent via a dialog instance.

```kotlin
/**
 * Sends a typing indicator to this dialog.
 *
 * Typically used to notify other participants that
 * the user is currently composing a message.
 *
 * @param request Optional typing parameters (preview text, timeout)
 * @param onComplete Callback invoked with the result of the operation
 */
fun sendTyping(
    request: TypingRequest = TypingRequest(),
    onComplete: (Result<Unit>) -> Unit
)
```

```kotlin
val request = TypingRequest(previewText = "Hey, are yo", timeoutMs = 5000)

dialog.sendTyping(request) { result ->
    result
        .onSuccess { }
        .onFailure { error -> }
}
```


## Receiving Typing Events

Typing state from other participants is delivered via realtime events as `ActivityEvent.Typing`:

```kotlin
data class Typing(
    override val dialogId: String,
    val member: Participant,
    val previewText: String? = null,
    val timeoutMs: Long? = null,
) : ActivityEvent()
```

- `member` — the participant who is typing
- `previewText` — preview of the text they're composing, if provided
- `timeoutMs` — how long the indicator should remain active; use it to decide when to clear the indicator from the UI if no follow-up event arrives

The SDK does not track typing state internally or start a timer — each event is dispatched once, and clearing the indicator is entirely up to the client.

```kotlin
override fun onEvent(event: ChatEvent) {
    when (event) {
        is ActivityEvent.Typing -> {
            // show typing indicator for event.member
        }
        else -> Unit
    }
}
```

See [Events](events.md) for how to register a listener.
