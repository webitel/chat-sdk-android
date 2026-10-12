package com.webitel.chat.sdk.internal.transport.dto

/** Item of a message's `failures` list. */
internal data class DeliveryFailureDto(
    val member: ParticipantDto,
    val error: DeliveryErrorDto
)


internal data class DeliveryErrorDto(
    val code: String,
    val message: String?
)
