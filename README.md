# Chat SDK for Android

An Android SDK for realtime chat messaging with WebSocket support, session management, and flexible authentication. Designed to work seamlessly with both Java and Kotlin.


---

## Installation

```gradle
dependencies {
    implementation("com.github.webitel:chat-sdk-android:<version>")
}
```

---

## Creating a ChatClient

Use the builder to configure and create a `ChatClient` instance.

```kotlin
val chatClient = ChatClient.builder(
    application = application,
    endpoint = "https://demo.webitel.com",
    clientToken = "client-token"
)
    .auth(AuthMethod.Token(tokenProvider))
    .logLevel(LogLevel.DEBUG)
    .deviceId("device-id")
    .networkConfig(NetworkConfig())
    .build()
```

---

## Authentication

The SDK supports multiple authentication strategies:

- Token-based authentication
- ContactIdentity-based authentication

For token-based authentication, the token is supplied via `AuthMethod.Token`.  
The SDK calls the provided `tokenProvider` whenever a token is required.

---

## Realtime Connection

Enable realtime updates by calling `connect()`:

```kotlin
chatClient.connect()
```

Disable realtime mode when no longer needed:

```kotlin
chatClient.disconnect()
```

Realtime connection state can be observed via `ConnectionListener`.

---

## Listening for Events

Register a global chat event listener:

```kotlin
chatClient.addEventListener { event ->
    when (event) {
        is MessageEvent.Received -> showMessage(event.message)
        is StateEvent.Typing -> showTyping(event.userId)
    }
}
```

Events are delivered only while realtime connection is active.

---

## Loading Dialogs

Retrieve dialogs for the current session:

```kotlin
chatClient.getDialogs { result ->
    result.onSuccess { dialogs ->
        showDialogs(dialogs)
    }
}
```

Each dialog is represented by a `Dialog` instance.

---

## Sending Messages

Send a message to a dialog:

```kotlin
val options = MessageOptions(text = "Hello")

dialog.sendMessage(options) { result ->
    result.onSuccess { message ->
        markAsSent(message.id)
    }
}
```

Sending does not require an active realtime connection.

---

## Receiving Messages

Incoming messages are delivered via `MessageEvent.Received` events when realtime is enabled.

---

## Loading Message History

```kotlin
dialog.getHistory(request) { result ->
    result.onSuccess { messages ->
        showHistory(messages)
    }
}
```

---

## Session Management

End the current server session explicitly:

```kotlin
chatClient.endSession()
```

This closes the backend session, stops push delivery, and closes realtime connections.

---

## Error Handling

All SDK errors are exposed as subclasses of `ChatError`.

```kotlin
result.onFailure { error ->
    if (error is ChatError.Unauthorized) {
        triggerLogin()
    }
}
```

Unknown server errors are wrapped as `ChatError.Unknown`.

---

## Security

### SSL Public Key Pinning

```kotlin
builder.pinnedPublicKeys(
    listOf(
        "sha256/AAAA...",
        "sha256/BBBB..."
    )
)
```

At least one pin must match the server certificate chain.

---

## License

MIT License