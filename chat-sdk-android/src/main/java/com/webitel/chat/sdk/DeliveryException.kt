package com.webitel.chat.sdk


/**
 * Failure to deliver a message to a specific dialog member
 * (e.g. the external channel rejected it).
 *
 * Available via [Message.deliveryExceptions] and [ReceiptEvent.DeliveryFailed].
 */
data class DeliveryException(

    /** Identifier of the message that was not delivered. */
    val messageId: String,

    /** Member the message was not delivered to. */
    val member: Participant,

    /** Error reported by the server or the external provider. */
    val error: DeliveryError
)


data class DeliveryError(

    /** Error code, e.g. `"403"`. */
    val code: String,

    /** Human-readable error description, if provided. */
    val message: String?
)
