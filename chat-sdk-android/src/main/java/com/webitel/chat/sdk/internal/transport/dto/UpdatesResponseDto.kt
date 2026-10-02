package com.webitel.chat.sdk.internal.transport.dto

internal data class UpdatesResponseDto(
    val cursor: String,
    val resync: Boolean,
    val hasMore: Boolean,
    val threads: List<ThreadUpdatesDto>
)


internal data class ThreadUpdatesDto(
    val threadId: String,
    val dialog: DialogDto?,
    val unreadCount: Int,
    val messages: List<MessageDto>,
    val topMessage: MessageDto?,
    val deletedMessageIds: List<String>,
    val readStates: List<ReadStateDto>,
    val memberChanges: List<MemberChangeDto>,
    val left: Boolean
)


internal data class ReadStateDto(
    val memberId: String?,
    val member: ParticipantDto?,
    val deliveredUpToSeq: Long,
    val readUpToSeq: Long
)


internal data class MemberChangeDto(
    val action: String,
    val member: ParticipantDto?,
    val by: ParticipantDto?
)
