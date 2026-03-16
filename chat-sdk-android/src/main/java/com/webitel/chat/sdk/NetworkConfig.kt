package com.webitel.chat.sdk


/**
 * Configuration for HTTP API requests.
 */
data class ApiConfig(
    /** Maximum time allowed for an API call before it times out. */
    val callTimeoutMs: Long = 5000L
)

/**
 * Configuration for realtime (WebSocket) connection behavior.
 */
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

/**
 * Root network configuration used by the chat client.
 */
data class NetworkConfig(
    /** HTTP API configuration. */
    val api: ApiConfig = ApiConfig(),

    /** Realtime WebSocket configuration. */
    val realtime: RealtimeConfig = RealtimeConfig()
)