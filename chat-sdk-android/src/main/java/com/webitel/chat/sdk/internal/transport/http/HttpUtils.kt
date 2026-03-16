package com.webitel.chat.sdk.internal.transport.http

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.internal.client.WLogger

internal inline fun <T> safeCall(
    logger: WLogger,
    tag: String,
    block: () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: ChatError) {
        Result.failure(e)
    } catch (e: Exception) {
        val error = ChatError.fromCode(ChatError.UNKNOWN_CODE, e.message)
        logger.error(tag, "safeCall: ${error.stackTrace}")
        Result.failure(error)
    }
}