package com.webitel.chat.sdk


sealed class ConnectionState {

    /**
     * SDK is trying to establish a realtime connection.
     */
    object Connecting : ConnectionState() {
        override fun toString() = "Connecting"
    }


    /**
     * Realtime connection is active.
     */
    object Connected : ConnectionState() {
        override fun toString() = "Connected"
    }


    /**
     * SDK is fully disconnected and will NOT reconnect automatically.
     * Happens after explicit disconnect().
     */
    object Disconnected : ConnectionState() {
        override fun toString() = "Disconnected"
    }


    /**
     * Connection was lost unexpectedly.
     * SDK will attempt to reconnect automatically.
     *
     * @param attempt reconnect attempt counter (1, 2, 3...)
     */
    data class Reconnecting(
        val attempt: Int,
        val maxAttempts: Int
    ) : ConnectionState() {
        override fun toString() = "Reconnecting(attempt: $attempt, maxAttempts: $maxAttempts)"
    }


    /**
    * Example:
    * ```
    * chatClient.addConnectionListener { state ->
    *     when (state) {
    *         is ConnectionState.Failed -> {
    *             showOfflineBanner(state.error)
    *         }
    *         ConnectionState.Connected -> hideOfflineBanner()
    *         else -> Unit
     *    }
    * }
    * ```
    */
    data class Failed(
        val error: ChatError
    ) : ConnectionState() {
        override fun toString() = "Failed(code: ${error.code}, message: ${error.message})"
    }
}
