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
     *     content = SendContent.Text("Hello")
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
     * Sends an action associated with a specific message.
     *
     * This is typically used for handling user interactions such as button clicks.
     * The action is delivered to the server and processed in the context of the
     * referenced message.
     *
     * @param messageId Identifier of the message the action relates to
     * @param action Action to be performed
     * @param onComplete Callback invoked with the result of the operation
     */
    fun sendAction(
        messageId: String,
        action: MessageAction,
        onComplete: (Result<Unit>) -> Unit
    )


    /**
     * Sets or clears the current user's reaction on a message.
     *
     * Only one reaction per user is allowed per message — sending a new
     * emoji replaces the previous one. Passing an empty [emoji] clears
     * the current user's reaction (see [removeReaction] for a convenience
     * shortcut).
     *
     * The [onComplete] callback only reports the outcome for the current
     * user's own reaction. The full aggregated reaction list for the
     * message is delivered separately via [MessageEvent.ReactionsChanged].
     *
     * @param messageId Identifier of the message to react to
     * @param emoji Emoji to react with, or an empty string to clear the reaction
     * @param sendId Optional client-generated identifier for request tracking
     * @param onComplete Callback invoked with the result of the operation
     */
    fun setReaction(
        messageId: String,
        emoji: String,
        sendId: String? = null,
        onComplete: (Result<ReactionResult>) -> Unit
    )


    /**
     * Deletes messages by identifier.
     *
     * This is a batch operation — it is not scoped to a particular dialog,
     * so ids from different dialogs may be passed together. Scoping and
     * authorization are enforced server-side: unauthorized or non-existent
     * ids are reported back via [MessageDeletionResult.skipped] instead of
     * failing the whole call.
     *
     * A realtime [MessageEvent.Deleted] is dispatched for each deleted
     * message, including deletions made by another participant.
     *
     * @param ids Identifiers of the messages to delete
     * @param onComplete Callback invoked with the result of the operation
     */
    fun deleteMessages(
        ids: List<String>,
        onComplete: (Result<MessageDeletionResult>) -> Unit
    )


    /**
     * Edits the text of an existing message.
     *
     * The [onComplete] callback only reports that the edit request was
     * accepted. The updated message content itself is delivered separately
     * via a realtime [MessageEvent.Edited], including edits made by another
     * participant.
     *
     * @param messageId Identifier of the message to edit
     * @param text New text content for the message
     * @param onComplete Callback invoked with the result of the operation
     */
    fun editMessage(
        messageId: String,
        text: String,
        onComplete: (Result<EditMessageResult>) -> Unit
    )


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


    /**
     * Returns an existing direct dialog for the specified contact,
     * or creates a new one if none exists.
     *
     * @param contactId The unique identifier of the contact.
     * @param onComplete Callback invoked with the dialog on success or an error on failure.
     */
    fun getOrCreateDialog(
        contactId: ContactId,
        onComplete: (Result<Dialog>) -> Unit
    )


    /**
     * Loads contacts available for the current session.
     *
     * Supports pagination via [ContactRequest].
     *
     * @param request Request parameters (pagination, filters, etc.)
     * @param onComplete Callback invoked with the loaded contacts or an error
     */
    fun getContacts(
        request: ContactRequest,
        onComplete: (Result<Page<Contact>>) -> Unit
    )


    /**
     * Starts an asynchronous file upload operation.
     *
     * Upload progress and completion events are delivered
     * through the provided [UploadListener].
     *
     * The returned [Cancellable] can be used to cancel
     * the upload at any time.
     *
     * If the upload was interrupted previously,
     * [UploadRequest.resumeId] can be used to continue it.
     */
    fun upload(
        request: UploadRequest,
        listener: UploadListener
    ): Cancellable


    /**
     * Starts an asynchronous file download operation.
     *
     * Downloaded file data is delivered incrementally
     * through [DownloadListener.onChunk].
     *
     * The returned [Cancellable] can be used to cancel
     * the download at any time.
     *
     * Downloads may optionally resume from a specific byte offset
     * using [DownloadRequest.offset].
     */
    fun download(
        request: DownloadRequest,
        listener: DownloadListener
    ): Cancellable


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
         * Enables automatic token refresh and request retry on `401 Unauthorized` responses.
         *
         * Default value is `true`.
         *
         * When enabled, the SDK will:
         * - Detect `401` responses from API requests
         * - Request a new JWT or Contact via `AuthMethod`
         * - Retry the original request once the token is refreshed and validated
         *
         * If the refreshed token is still invalid, the original request will fail with `401`.
         */
        fun autoRefreshAuth(value: Boolean): Builder


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