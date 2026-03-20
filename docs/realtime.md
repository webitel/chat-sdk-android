# Realtime

Realtime functionality is implemented using WebSocket.


## Connect

```kotlin
chatClient.connect()
```
After calling:
- WebSocket connection is established  
- realtime events are enabled  
- automatic reconnection is handled  


## Configuration
```kotlin
data class RealtimeConfig(
    /** Maximum number of reconnect attempts before giving up. */
    val maxRetries: Int = 10,

    /** Interval between ping frames to keep the connection alive. */
    val pingIntervalMs: Long = 10000L,

    /** Base delay used for calculating reconnect backoff. */
    val retryBaseDelayMs: Long = 500L,

    /** Maximum delay between reconnect attempts. */
    val maxRetryDelayMs: Long = 10000L
)
```


### Reconnect behavior

- automatic reconnect attempts are performed  
- backoff strategy is used  
- after exceeding maxRetries, state becomes Failed  

In Failed state, automatic reconnects stop.

To restart:
```kotlin
chatClient.connect()
```
Retry counter resets after this call.  


## Connection state listener
```kotlin
chatClient.addConnectionListener(this)
```

States:
- Connecting
- Connected
- Disconnected
- Reconnecting
- Failed  


## Disconnect

```kotlin
chatClient.disconnect()
```
