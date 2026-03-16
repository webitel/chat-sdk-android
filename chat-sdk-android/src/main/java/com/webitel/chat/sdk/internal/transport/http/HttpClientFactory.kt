package com.webitel.chat.sdk.internal.transport.http

import com.webitel.chat.sdk.internal.client.ClientContext
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal class HttpClientFactory(
    private val clientContext: ClientContext,
    private val headerProvider: HeaderProvider
) {

    fun create(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .callTimeout(clientContext.networkConfig.api.callTimeoutMs,
                TimeUnit.MILLISECONDS)
            .pingInterval(clientContext.networkConfig.api.callTimeoutMs,
                TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(HeaderInterceptor(headerProvider))
        applyCertificatePinning(builder)

        return builder.build()
    }


    private fun applyCertificatePinning(builder: OkHttpClient.Builder) {
        if (clientContext.pinnedHashes.isEmpty()) return

        val normalizedPins = clientContext.pinnedHashes
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { normalizeSha256Pin(it) }
            .toList()

        if (normalizedPins.isEmpty()) return

        val certificatePinner = CertificatePinner.Builder()
            .add(clientContext.host, *normalizedPins.toTypedArray())
            .build()

        builder.certificatePinner(certificatePinner)
    }


    private fun normalizeSha256Pin(pin: String): String =
        if (pin.startsWith("sha256/")) pin else "sha256/$pin"
}