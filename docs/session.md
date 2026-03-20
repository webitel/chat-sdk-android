# Session

End session:

```kotlin
chatClient.endSession()
```

Behavior  
- user session is terminated  
- WebSocket connection is closed (if active)  
- user is logged out on the server  
- push notifications are no longer delivered to the device  