package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.MemberChange
import com.webitel.chat.sdk.MemberChangeAction
import com.webitel.chat.sdk.Participant
import com.webitel.chat.sdk.ParticipantState
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.transport.dto.MemberChangeDto
import com.webitel.chat.sdk.internal.transport.dto.ReadStateDto

private const val TAG = "UpdatesDto"


/**
 * Resolves the participant from the embedded `member`, or by `member_id`
 * among [members] when the server sends only the id.
 */
internal fun ReadStateDto.toDomain(members: List<Participant> = emptyList()): ParticipantState? {
    val participant = member?.toDomain()
        ?: memberId?.let { id -> members.firstOrNull { it.id == id } }

    if (participant == null) {
        logger.warn(TAG, "Read state skipped, unknown member ${memberId ?: "-"}")
        return null
    }

    return ParticipantState(
        member = participant,
        deliveredUpToSequence = deliveredUpToSeq,
        readUpToSequence = readUpToSeq
    )
}


internal fun MemberChangeDto.toDomain(): MemberChange? {
    val member = member ?: return null

    return MemberChange(
        action = memberChangeActionFrom(action),
        member = member.toDomain(),
        by = by?.toDomain()
    )
}


private fun memberChangeActionFrom(raw: String): MemberChangeAction {
    val value = raw.uppercase()

    if (value.endsWith("JOINED")) {
        return MemberChangeAction.Added
    }

    if (value.endsWith("LEFT")) {
        return MemberChangeAction.Removed
    }

    return MemberChangeAction.Unknown(raw)
}
