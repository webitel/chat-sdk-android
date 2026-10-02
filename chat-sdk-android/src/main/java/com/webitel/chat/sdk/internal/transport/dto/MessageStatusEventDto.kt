package com.webitel.chat.sdk.internal.transport.dto

internal data class MessageStatusEventDto(
    val dialogId: String,
    val status: String,
    val member: ParticipantDto,
    val upToSeq: Long,
    val occurredAt: Long?
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
