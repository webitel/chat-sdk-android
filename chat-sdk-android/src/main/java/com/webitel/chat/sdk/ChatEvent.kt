package com.webitel.chat.sdk


/**
 * Base interface for all chat-related events emitted by the SDK.
 *
 * All events are scoped to a dialog via [dialogId].
 *
 * Clients usually subscribe once (e.g. on ChatClient)
 * and route events to UI or local storage.
 *
 * Example:
 * ```
 * chatClient.addEventListener { event ->
 *     when (event) {
 *         is MessageEvent -> handleMessageEvent(event)
 *         is ParticipantEvent -> handleParticipantEvent(event)
 *         is StateEvent -> handleStateEvent(event)
 *     }
 * }
 * ```
 */
sealed interface ChatEvent {
    val dialogId: String
}


/**
 * Events related to messages lifecycle.
 *
 * Covers receiving new messages as well as updates to existing ones.
 */
sealed class MessageEvent : ChatEvent {

    /**
     * Emitted when the SDK receives a message from the server.
     *
     * This includes:
     * - real-time incoming messages
     * - messages sent by the current user (after server confirmation)
     * - replayed messages after reconnect
     *
     * Example usage:
     * ```
     * when (event) {
     *     is MessageEvent.Received -> {
     *         messagesAdapter.add(event.message)
     *         updateLastMessage(event.dialogId, event.message)
     *     }
     * }
     * ```
     */
    data class Received(
        override val dialogId: String,
        val message: Message,
    ) : MessageEvent()

    /**
     * Emitted when an existing message was edited.
     *
     * Clients should update message content locally
     * without creating a new message item.
     *
     * Example usage:
     * ```
     * when (event) {
     *     is MessageEvent.Edited -> {
     *         messagesAdapter.updateText(
     *             messageId = event.messageId,
     *             newText = event.newText
     *         )
     *     }
     * }
     * ```
     */
    data class Edited(
        override val dialogId: String,
        val messageId: String,
        val newText: String,
    ) : MessageEvent()

    /**
     * Emitted when a message was deleted.
     *
     * Clients may:
     * - remove the message from UI
     * - or replace it with a "Message deleted" placeholder
     *
     * Example usage:
     * ```
     * when (event) {
     *     is MessageEvent.Deleted -> {
     *         messagesAdapter.remove(event.messageId)
     *     }
     * }
     * ```
     */
    data class Deleted(
        override val dialogId: String,
        val messageId: String,
    ) : MessageEvent()
}


/**
 * Events related to dialog lifecycle.
 */
sealed class DialogEvent : ChatEvent {

    /**
     * Emitted when a new dialog is created.
     *
     * This can happen when:
     * - the current user sends the first message to a contact
     * - the server creates a dialog automatically
     * - a dialog is assigned by an operator/bot
     *
     * Clients should:
     * - add the dialog to the dialogs list
     * - optionally open it if it was created from the current screen
     */
    data class Created(
        override val dialogId: String,
        val dialog: Dialog
    ) : DialogEvent()
}


/**
 * Events describing transient dialog state.
 *
 * These events usually should NOT be persisted.
 */
sealed class StateEvent : ChatEvent {

    /**
     * Emitted when a participant is typing.
     *
     * Typically used to show a typing indicator in UI.
     *
     * Example usage:
     * ```
     * when (event) {
     *     is StateEvent.Typing -> {
     *         typingIndicator.show(event.userId)
     *     }
     * }
     * ```
     */
    data class Typing(
        override val dialogId: String,
        val userId: String,
    ) : StateEvent()

    /**
     * Emitted when a message was marked as read by a participant.
     *
     * Example usage:
     * ```
     * when (event) {
     *     is StateEvent.Read -> {
     *         messagesAdapter.markAsRead(
     *             messageId = event.messageId,
     *             userId = event.userId
     *         )
     *     }
     * }
     * ```
     */
    data class Read(
        override val dialogId: String,
        val messageId: String,
        val userId: String,
    ) : StateEvent()
}