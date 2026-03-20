# Push Notifications

To receive push notifications, register a device.

## Registration

```kotlin
requireChatClient.registerDevice(token) { result ->
    result
        .onSuccess { /* Device registered */ }
        .onFailure { error -> }
}
```

Re-register on token refresh.
