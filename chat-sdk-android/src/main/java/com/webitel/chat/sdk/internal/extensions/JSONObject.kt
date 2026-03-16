package com.webitel.chat.sdk.internal.extensions

import org.json.JSONObject

internal fun JSONObject.longOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null
