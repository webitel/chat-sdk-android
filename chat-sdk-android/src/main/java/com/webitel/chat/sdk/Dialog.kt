package com.webitel.chat.sdk


/**
 * Represents a chat dialog (conversation).
 */
interface Dialog {

    /** Unique identifier of the dialog. */
    val id: String

    /** Display name or subject of the dialog. */
    val subject: String

    /** Type of the dialog (direct, group, etc.). */
    val type: DialogType

    /** List of dialog participants. */
    val members: List<Participant>

    /** Last message sent in the dialog, if available. */
    val lastMessage: Message?


    /**
     * Sends a message to this dialog asynchronously.
     *
     * This method does NOT require an active realtime connection.
     * If realtime is disabled or disconnected, the SDK will perform
     * a one-off authenticated request to send the message.
     *
     * When realtime is active, message delivery confirmations and
     * further updates (edited, read, reactions, etc.) will be delivered
     * via [ChatEventListener].
     *
     * The returned [Cancellable] allows cancelling the send operation
     * (for example, during file upload or before the request completes).
     *
     * The [onComplete] callback is invoked when the server responds to
     * the send request. A successfully sent message will also be emitted
     * later as [MessageEvent.Received].
     *
     * Example:
     * ```
     * val options = MessageOptions(
     *     text = "Hello"
     * )
     *
     * val handle = dialog.sendMessage(options) { result ->
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
     * @param options Message payload
     * @param onComplete Callback invoked with the result of the send operation
     *
     * @return A [Cancellable] handle for cancelling the operation
     */
    fun sendMessage(
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable


    /**
     * Loads message history for this dialog.
     *
     * The request may be cancelled if the result is no longer needed
     * (for example, when the UI is destroyed).
     *
     * @param request History request parameters (pagination, direction, limits)
     * @param onComplete Callback invoked with the loaded messages or an error
     *
     */
    fun getHistory(
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<Message>>) -> Unit
    )


    /**
     * Initiates a file download associated with this dialog.
     *
     * File data is delivered in chunks to the provided [DownloadListener].
     * The SDK does NOT persist the file on disk — the client is responsible
     * for handling storage, caching, or in-memory processing.
     *
     * @param fileId The unique identifier of the file
     * @param listener Listener receiving file chunks and completion events
     *
     * @return A [Cancellable] handle for cancelling the download
     */
    fun downloadFile(
        fileId: String,
        listener: DownloadListener
    ): Cancellable


    /**
     * Initiates a file download starting from a specific byte offset.
     *
     * This is useful for resuming interrupted downloads.
     *
     * @param fileId The unique identifier of the file
     * @param offset Byte offset to start downloading from
     * @param listener Listener receiving file chunks and completion events
     *
     * @return A [Cancellable] handle for cancelling the download
     */
    fun downloadFile(
        fileId: String,
        offset: Long,
        listener: DownloadListener
    ): Cancellable


    /**
     * Registers a dialog-scoped chat event listener.
     *
     * The listener will receive only events related to this dialog
     * (messages, typing indicators, read receipts, participant changes).
     *
     * Intended for:
     *  - dialog screen UI
     *  - message list updates
     *  - typing indicators
     */
    fun addListener(listener: ChatEventListener)


    /**
     * Unregisters a previously added dialog-scoped event listener.
     */
    fun removeListener(listener: ChatEventListener)
}