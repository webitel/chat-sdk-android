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
     *     content = SendContent.Text("Hello")
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