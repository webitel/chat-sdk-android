package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageStatusEventDto(
    val dialogId: String,
    val status: String,
    val member: ParticipantDto,
    /** Receipt horizon, present for `read` / `delivered` statuses. */
    val upToSeq: Long?,
    /** Failed message, present for the `failed` status. */
    val messageId: String?,
    /** Delivery error, present for the `failed` status. */
    val error: DeliveryErrorDto?,
    val occurredAt: Long?,
    /** Current user's unread count, present only when the event is about the current user. */
    val unreadCount: Int?
) {

    /** Receipt kind for the status, null for statuses not handled as receipts. */
    val receiptKind: ReceiptKind?
        get() = when (status.lowercase()) {
            "read" -> ReceiptKind.READ
            "delivered" -> ReceiptKind.DELIVERED
            else -> null
        }

    /** Delivery failure for the `failed` status, null otherwise or when the payload is incomplete. */
    val deliveryFailure: DeliveryFailureDto?
        get() {
            if (status.lowercase() != "failed") return null
            if (messageId == null) return null
            val error = error ?: return null

            return DeliveryFailureDto(member = member, error = error)
        }
}


internal enum class ReceiptKind {
    DELIVERED,
    READ
}
