# SDK Initialization

The entry point to the SDK is the `ChatClient` class.

```kotlin
ChatClient.builder(application, endpoint, clientToken)
    .auth(authMethod(user))
    .logLevel(LogLevel.DEBUG)
    .build()
    .also {
        it.addEventListener(this)
        it.addConnectionListener(this)
    }
```


## Required parameters

- `endpoint` — server connection URL  
- `clientToken` — client identifier generated on the server  
- `auth` — authentication method  

Authentication is performed automatically. No explicit login method is required.


## Optional parameters

- `logLevel` — logging level  
- `deviceId` — unique device identifier  
- `networkConfig` — network configuration  


## Network configuration

Allows configuring HTTP and WebSocket separately.

```kotlin
data class NetworkConfig(
    /** HTTP API configuration. */
    val api: ApiConfig = ApiConfig(),

    /** Realtime WebSocket configuration. */
    val realtime: RealtimeConfig = RealtimeConfig()
)
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
