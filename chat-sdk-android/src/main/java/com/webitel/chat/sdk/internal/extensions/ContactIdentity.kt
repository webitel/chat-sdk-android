package com.webitel.chat.sdk.internal.extensions

import com.webitel.chat.sdk.ContactIdentity
import org.json.JSONObject
import kotlin.collections.component1
import kotlin.collections.component2

internal fun ContactIdentity.toJsonObject(): JSONObject {
    return JSONObject().apply {
        put("sub", sub)
        put("iss", iss)
        put("name", name)

        givenName?.let { put("given_name", it) }
        middleName?.let { put("middle_name", it) }
        familyName?.let { put("family_name", it) }
        email?.let { put("email", it) }
        emailVerified?.let { put("email_verified", it) }
        phoneNumber?.let { put("phone_number", it) }
        phoneNumberVerified?.let { put("phone_number_verified", it) }
        birthdate?.let { put("birthdate", it) }
        gender?.let { put("gender", it) }
        locale?.let { put("locale", it) }
        zoneinfo?.let { put("zoneinfo", it) }

        metadata?.let { map ->
            val metaJson = JSONObject()
            map.forEach { (key, value) ->
                when (value) {
                    null -> metaJson.put(key, JSONObject.NULL)
                    is String, is Number, is Boolean -> metaJson.put(key, value)
                    else -> metaJson.put(key, value.toString())
                }
            }
            put("metadata", metaJson)
        }
    }
}