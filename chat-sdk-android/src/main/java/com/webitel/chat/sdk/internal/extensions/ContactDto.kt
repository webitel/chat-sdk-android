package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.Contact
import com.webitel.chat.sdk.ContactId
import com.webitel.chat.sdk.internal.transport.dto.ContactDto

internal fun ContactDto.toDomain(): Contact =
    Contact(
        id = ContactId(id, iss),
        name = name,
        source = source,
        isBot = isBot
    )