package com.webitel.chat.sdk


/**
 * Base error type for Chat SDK.
 *
 * Unknown or unmapped error codes are wrapped into [ChatError.Unknown].
 */
sealed class ChatError(
    val code: Int,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Authentication failed.
     * Client should trigger login / re-auth flow.
     */
    object Unauthorized : ChatError(
        code = 401,
        message = "Unauthorized"
    )

    /**
     * Access is forbidden for current user.
     */
    object Forbidden : ChatError(
        code = 403,
        message = "Forbidden"
    )

    object Timeout : ChatError(
        code = 408,
        message = "Request timeout"
    )

    /**
     * Requested resource was not found.
     */
    object NotFound : ChatError(
        code = 404,
        message = "Not found"
    )

    object Conflict : ChatError(
        code = 409,
        message = "Conflict"
    )

    class InternalServerError(val messageOverride: String? = null) : ChatError(
        code = 500,
        message = messageOverride ?: "Internal server error"
    )

    object NotImplemented : ChatError(
        code = 501,
        message = "Not implemented"
    )

    object ServiceUnavailable : ChatError(
        code = 503,
        message =  "Service unavailable"
    )

    /**
     * Server returned a known error code that does not have
     * a dedicated typed error yet.
     */
    class Unknown(
        code: Int,
        message: String,
        cause: Throwable? = null
    ) : ChatError(
        code = code,
        message = message,
        cause = cause
    )

    companion object {

        /**
         * Maps raw server error codes to typed [ChatError].
         *
         * @param code Raw error/status code from backend
         * @param messageOverride Optional server-provided message
         * @param cause Optional underlying exception
         *
         * @return Typed [ChatError] instance
         */
        fun fromCode(
            code: Int,
            messageOverride: String? = null,
            cause: Throwable? = null
        ): ChatError {
            return when (code) {
                401 -> Unauthorized
                403 -> Forbidden
                404 -> NotFound
                408 -> Timeout
                409 -> Conflict
                500 -> InternalServerError(messageOverride)
                501 -> NotImplemented
                503 -> ServiceUnavailable
                else -> Unknown(
                    code = code,
                    message = messageOverride ?: "Unmapped error code: $code",
                    cause = cause
                )
            }
        }

        const val UNKNOWN_CODE = -1
    }
}