package com.webitel.chat.sdk.internal.transport.parser

import java.math.BigDecimal
import java.math.BigInteger


/**
 * Normalizes an updates cursor sent either as a JSON string or a number,
 * so cursors from realtime frames and the updates API compare equal.
 *
 * Large numbers parsed by org.json as `Double` are converted back to their
 * integer form instead of the scientific notation of `Double.toString()`.
 */
internal fun normalizeCursor(value: Any?): String? =
    when (value) {
        is String -> value.takeIf { it.isNotEmpty() }
        is Int, is Long, is BigInteger -> value.toString()
        is Double, is Float, is BigDecimal -> runCatching {
            BigDecimal(value.toString()).toBigIntegerExact().toString()
        }.getOrNull()
        else -> null
    }
