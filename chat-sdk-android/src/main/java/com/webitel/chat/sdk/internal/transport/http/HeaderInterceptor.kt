package com.webitel.chat.sdk.internal.transport.http

import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

internal class HeaderInterceptor(
    private val headerProvider: HeaderProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        try {
            headerProvider.commonHeaders().forEach { (key, value) ->
                builder.header(key, value)
            }
        } catch (e: IllegalArgumentException) {
            logger.error("HeaderInterceptor", "Invalid header value: ${e.message}")

            return Response.Builder()
                .request(original)
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Client Error: Invalid characters in headers. ${e.message}")
                .body("{\"error\": \"invalid_headers: ${e.message}\"}"
                    .toResponseBody("application/json".toMediaType()))
                .build()
        }

        return chain.proceed(builder.build())
    }
}