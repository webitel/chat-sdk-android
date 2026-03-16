package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.AuthMethod
import com.webitel.chat.sdk.LogLevel
import com.webitel.chat.sdk.NetworkConfig

data class ClientContext(
    val host: String,
    val port: Int,
    val scheme: String,
    val clientToken: String,
    val logLevel: LogLevel,
    val networkConfig: NetworkConfig,
    val authMethod: AuthMethod,
    val deviceId: String,
    val agent: String,
    var pinnedHashes: Set<String>
)
