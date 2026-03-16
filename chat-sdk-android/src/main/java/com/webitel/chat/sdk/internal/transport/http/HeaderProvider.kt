package com.webitel.chat.sdk.internal.transport.http

import com.webitel.chat.sdk.internal.client.ClientContext

internal class HeaderProvider(
    private val client: ClientContext
) {
    @Volatile
    private var accessToken: String? = null

    fun updateAccessToken(token: String?) {
        accessToken = token
    }

    fun commonHeaders(): Map<String, String> {
        val map = mutableMapOf(
            "x-webitel-device" to client.deviceId,
            "x-webitel-client" to client.clientToken,
            "User-Agent" to client.agent
        )

        accessToken?.takeIf { it.isNotBlank() }?.let {
            map["x-webitel-access"] = it
        }

        return map
    }

    fun hasAuth(): Boolean {
        return !accessToken.isNullOrEmpty()
    }
}