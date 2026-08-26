package com.webitel.chat.sdk.internal.transport.realtime

internal enum class EventType(val value: String) {
    Connected("connected_event"),
    Disconnected("disconnected_event"),
    DialogCreated("thread_created_event"),
    Message("message_event"),
    Typing("typing_event"),
    MessageReaction("message_reaction_event"),
    MessageDeleted("message_deleted_event"),
    MessageEdited("message_edited_event"),
    Ack("ack_event"),
    Error("error_event"),
    Ping("ping_event"),
    Unsupported("unsupported_event");

    companion object {
        fun from(value: String): EventType =
            EventType.entries.firstOrNull { it.value == value }
                ?: Unsupported
    }
}