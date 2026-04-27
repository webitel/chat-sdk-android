package com.webitel.chat.sdk.internal.client

import android.app.Application
import androidx.core.net.toUri
import com.webitel.chat.sdk.AuthMethod
import com.webitel.chat.sdk.ChatClient
import com.webitel.chat.sdk.LogLevel
import com.webitel.chat.sdk.NetworkConfig
import com.webitel.chat.sdk.internal.auth.HttpAuthManager
import com.webitel.chat.sdk.internal.api.HttpChatApiDelegate
import com.webitel.chat.sdk.internal.transport.http.HeaderProvider
import com.webitel.chat.sdk.internal.transport.realtime.WssRealtimeTransport
import com.webitel.chat.sdk.internal.repository.DeviceInfoRepository
import com.webitel.chat.sdk.internal.repository.storage.DeviceInfoStorageSharedPref
import com.webitel.chat.sdk.internal.transport.http.HttpClientFactory

internal class ChatClientBuilder(
    private val application: Application,
    private val endpoint: String,
    private val clientToken: String
) : ChatClient.Builder {

    private val deviceInfoRepository = DeviceInfoRepository(
        application,
        DeviceInfoStorageSharedPref(
            application
        )
    )

    private var authMethod: AuthMethod? = null
    private var deviceId: String? = null
    private var logLevel: LogLevel = LogLevel.ERROR
    private var networkConfig = NetworkConfig()
    private var pinnedPublicKeyHashes: Set<String> = emptySet()

    override fun auth(method: AuthMethod) = apply {
        this.authMethod = method
    }

    override fun logLevel(value: LogLevel) = apply {
        this.logLevel = value
    }

    override fun pinnedPublicKeys(pins: Collection<String>) = apply {
        this.pinnedPublicKeyHashes = pins.toSet()
    }

    override fun deviceId(value: String) = apply {
        this.deviceId = value
    }

    override fun networkConfig(config: NetworkConfig) = apply {
        this.networkConfig = config
    }

    override fun build(): ChatClient {
        val authMethod = requireNotNull(authMethod) {
            "AuthProvider is required"
        }

        val uri = endpoint.toUri()
        val host = uri.host
        if (host.isNullOrEmpty())
            throw Exception("Bad address - $endpoint")

        val clientContext = ClientContext(
            host,
            uri.port,
            uri.scheme ?: "https",
            clientToken,
            logLevel,
            networkConfig,
            authMethod,
            deviceId ?: deviceInfoRepository.getDeviceId(),
            deviceInfoRepository.getUserAgent(),
            pinnedPublicKeyHashes
        )

        val realtimeHub = RealtimeHub()
        val execution = ExecutionContext()
        val headerProvider = HeaderProvider(clientContext)

        val httpClient = HttpClientFactory(clientContext, headerProvider).create()

        val authManager = HttpAuthManager(
            clientContext,
            execution,
            httpClient,
            headerProvider
        )

        val apiDelegate = HttpChatApiDelegate(
            clientContext,
            execution,
            httpClient
        )

        val realtimeTransport = WssRealtimeTransport(
            clientContext,
            execution,
            headerProvider
        )

        authManager.addTokenListener { token ->
            realtimeTransport.onAuthUpdated(token)
        }

        return ChatClientImpl(
            clientContext,
            apiDelegate,
            authManager,
            realtimeTransport,
            realtimeHub
        )
    }
}