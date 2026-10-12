package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.DeliveryError
import com.webitel.chat.sdk.DeliveryException
import com.webitel.chat.sdk.internal.transport.dto.DeliveryErrorDto
import com.webitel.chat.sdk.internal.transport.dto.DeliveryFailureDto

internal fun DeliveryFailureDto.toDomain(messageId: String): DeliveryException {
    return DeliveryException(
        messageId = messageId,
        member = member.toDomain(),
        error = error.toDomain()
    )
}


internal fun DeliveryErrorDto.toDomain(): DeliveryError {
    return DeliveryError(
        code = code,
        message = message
    )
}
