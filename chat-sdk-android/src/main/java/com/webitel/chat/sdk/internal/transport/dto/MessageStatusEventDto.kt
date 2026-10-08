package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageStatusEventDto(
    val dialogId: String,
    val status: String,
    val member: ParticipantDto,
    val upToSeq: Long,
    val occurredAt: Long?,
    /** Current user's unread count, present only when the event is about the current user. */
    val unreadCount: Int?
) {

    /** Receipt kind for the status, null for statuses not handled by this SDK version. */
    val receiptKind: ReceiptKind?
        get() = when (status.lowercase()) {
            "read" -> ReceiptKind.READ
            "delivered" -> ReceiptKind.DELIVERED
            else -> null
        }
}


internal enum class ReceiptKind {
    DELIVERED,
    READ
}
