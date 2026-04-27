package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.Participant
import com.webitel.chat.sdk.ParticipantRole
import com.webitel.chat.sdk.internal.transport.dto.ParticipantDto


internal fun ParticipantDto.toDomain(): Participant =
    Participant(
        id = id,
        contact = contact.toDomain(),
        role = ParticipantRole.fromValue(role)
    )