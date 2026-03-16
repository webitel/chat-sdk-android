package com.webitel.chat.sdk

import android.app.Application
import com.webitel.chat.sdk.internal.client.ChatClientBuilder


interface ChatClient {

    val connectionState: ConnectionState

    /**
     * Sends a message to a dialog asynchronously.
     *
     * This method does NOT require an active realtime connection.
     * If the client is not connected, the SDK will perform a one-off
     * authenticated request to send the message.
     *
     * When realtime is active, message delivery confirmations and
     * further updates (edited, read, reactions, etc.) will be delivered
     * via [ChatEventListener].
     *
     * The returned [Cancellable] allows cancelling the send operation
     * (for example, during file upload or before the request is completed).
     *
     * The [onComplete] callback is invoked when the server responds to
     * the send request. A successfully sent message will be emitted
     * later as [MessageEvent.Received].
     *
     * Example:
     * ```
     * val options = MessageOptions(
     *     text = "Hello"
     * )
     *
     * val handle = chatClient.sendMessage(target, options) { result ->
     *     result
     *         .onSuccess { messageId ->
     *             // Server acknowledged the message
     *             markAsSent(messageId)
     *         }
     *         .onFailure { error ->
     *             showError(error)
     *         }
     * }
     *
     * // Cancel sending if needed
     * handle.cancel()
     * ```
     *
     * @param target Target Contact or Dialog
     * @param options Message payload and metadata
     * @param onComplete Callback invoked with the result of the send operation
     *
     * @return A [Cancellable] handle for cancelling the operation
     */
    fun sendMessage(
        target: MessageTarget,
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable


    /**
     * Loads dialogs available for the current session.
     *
     * This method retrieves the list of dialogs the current user
     * participates in and returns them as [Dialog] instances.
     *
     * Dialog updates (new messages, typing indicators, etc.) will be
     * delivered via [ChatEventListener] when realtime is active.
     *
     * Example:
     * ```
     * chatClient.getDialogs(request) { result ->
     *     result
     *         .onSuccess { dialogs ->
     *             showDialogs(dialogs)
     *         }
     *         .onFailure { error ->
     *             showError(error)
     *         }
     * }
     * ```
     *
     * @param onComplete Callback invoked with the loaded dialogs or an error
     *
     */
    fun getDialogs(
        request: DialogRequest,
        onComplete: (Result<Page<Dialog>>) -> Unit
    )


    fun getContacts(
        request: ContactRequest,
        onComplete: (Result<Page<Contact>>) -> Unit
    )


    /**
     * Registers a global chat event listener.
     *
     * The listener will receive all chat-related events across
     * all dialogs, including messages, participant updates,
     * typing indicators, read receipts, and connection-related events.
     *
     * Intended for:
     *  - global UI (chat list, unread counters)
     *  - analytics
     *  - logging
     */
    fun addEventListener(listener: ChatEventListener)

    /**
     * Unregisters a previously added global chat event listener.
     */
    fun removeEventListener(listener: ChatEventListener)


    /**
     * Starts realtime connection and enables persistent realtime mode.
     *
     * When connected, the SDK maintains a WebSocket connection and
     * automatically reconnects when:
     *  - network changes
     *  - authentication expires
     *  - transient server errors occur
     *
     * Token refresh is performed via the configured authentication method.
     *
     * This method is idempotent — calling it multiple times has no effect
     * if the connection is already active.
     *
     * Client MUST call [disconnect] when realtime updates are no longer needed.
     */
    fun connect()

    /**
     * Stops realtime connection and disables auto-reconnect.
     *
     * After calling this method:
     *  - WebSocket connection is closed
     *  - realtime events will no longer be delivered
     *  - messages may still arrive via push notifications (if configured)
     *
     * This does NOT end the user session on the server.
     */
    fun disconnect()


    /**
     * Ends the current user session on the backend.
     *
     * This explicitly invalidates the session associated with the
     * current authentication context.
     *
     * After calling this method:
     *  - server session is closed
     *  - push notifications stop
     *  - realtime connection (if active) is closed
     *
     * The SDK remains usable and may automatically re-authenticate
     * on the next operation (e.g. [connect] or [sendMessage]).
     *
     * Result indicating whether the session termination succeeded
     */
    fun endSession(onComplete: (Result<Unit>) -> Unit)


    /**
     * Registers the device for push notifications with the provided token.
     * @param pushToken The push token for the device.
     * @param onComplete The callback to handle the registration result.
     */
    fun registerDevice(pushToken: String, onComplete: (Result<Unit>) -> Unit)


    /**
     * Registers a connection state listener.
     *
     * The listener will be notified about realtime connection state changes,
     * such as connecting, connected, reconnecting, disconnected, and failures.
     *
     * Intended for:
     *  - UI indicators
     *  - diagnostics
     *  - logging
     */
    fun addConnectionListener(listener: ConnectionListener)

    /**
     * Unregisters a previously added connection state listener.
     */
    fun removeConnectionListener(listener: ConnectionListener)


    companion object {

        /**
         * Creates a new [ChatClient] builder instance.
         *
         * @param application Application context
         * @param endpoint Base API endpoint
         * @param clientToken Client identifier or static token
         */
        fun builder(
            application: Application,
            endpoint: String,
            clientToken: String
        ): Builder = ChatClientBuilder(application, endpoint, clientToken)
    }


    interface Builder {

        /**
         * Configures authentication strategy for the client.
         *
         * This may be JWT-based or ContactIdentity-based
         */
        fun auth(method: AuthMethod): Builder


        /**
         * Sets the log level for SDK internal logging.
         *
         * Available levels (ascending order):
         *  - debug — all messages
         *  - info — informational, warning, and error messages
         *  - warn — warning and error messages
         *  - error — error messages only
         *  - off — disables all logging
         *
         * Default value: [LogLevel.ERROR]
         */
        fun logLevel(value: LogLevel): Builder


        /**
         * Configures SSL/TLS public key pinning for all network connections
         * created by the SDK.
         *
         * @param pins A collection of Base64-encoded SHA-256 public key hashes (SPKI).
         *             At least one provided pin must match the server certificate
         *             chain for the connection to be trusted.
         */
        fun pinnedPublicKeys(pins: Collection<String>): Builder


        /**
         * Sets a unique device identifier used for session tracking.
         */
        fun deviceId(value: String): Builder


        fun networkConfig(config: NetworkConfig): Builder


        /**
         * Builds and returns an immutable [ChatClient] instance.
         */
        fun build(): ChatClient
    }
}