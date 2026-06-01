package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.DownloadListener
import com.webitel.chat.sdk.DownloadRequest
import com.webitel.chat.sdk.DownloadResult
import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.client.ClientContext
import com.webitel.chat.sdk.internal.client.ExecutionContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit


internal class HttpFileDownloader(
    private val clientContext: ClientContext,
    private val execution: ExecutionContext,
    private val httpClient: OkHttpClient
) {

    private companion object {
        const val TAG = "HttpFileDownloader"
        const val BUFFER_SIZE = 4 * 1024
        const val DOWNLOAD_PATH = "im/media"
    }


    fun download(
        request: DownloadRequest,
        listener: DownloadListener
    ): Cancellable {
        val task = TransferTaskImpl()
        execution.transfer {
            try {
                val httpRequest = buildRequest(request)
                logger.debug(TAG, "download: ${httpRequest.url}")

                val call = downloadClient.newCall(httpRequest)
                task.call = call

                call.execute().use { response ->
                    val body = response.body
                        ?: throw IOException("Empty response body")

                    if (!response.isSuccessful) {
                        val errorBody = body.string()
                        logger.error(
                            TAG,
                            "download failed: ${response.code} $errorBody"
                        )

                        throw ChatError.fromCode(
                            response.code,
                            errorBody
                        )
                    }

                    streamResponseBody(
                        body = body,
                        listener = listener,
                        task = task
                    )
                }

            } catch (e: Exception) {
                if (e == ChatError.Canceled || task.isCanceled()) {
                    logger.debug(TAG, "download canceled")
                    listener.onError(ChatError.Canceled)
                    return@transfer
                }

                logger.error(
                    TAG,
                    "download failed: ${e.stackTraceToString()}"
                )
                listener.onError(e.toChatError())
            }
        }
        return task
    }


    private fun buildRequest(
        request: DownloadRequest
    ): Request {
        val url = HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .apply {
                if (clientContext.port > 0) {
                    port(clientContext.port)
                }
            }
            .addPathSegments(DOWNLOAD_PATH)
            .addPathSegments(request.fileId)
            .addPathSegments("stream")
            .build()

        return Request.Builder()
            .url(url)
            .apply {
                if (request.offset > 0) {
                    logger.debug(TAG, "Range: bytes=${request.offset}-")
                    addHeader(
                        "Range",
                        "bytes=${request.offset}-"
                    )
                }
            }
            .get()
            .build()
    }


    private fun streamResponseBody(
        body: ResponseBody,
        listener: DownloadListener,
        task: TransferTaskImpl
    ) {
        val totalBytes = body.contentLength()
            .takeIf { it > 0 }

        logger.debug(
            TAG,
            "download started: total=$totalBytes"
        )

        val buffer = ByteArray(BUFFER_SIZE)
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            while (true) {
                if (task.isCanceled()) {
                    logger.debug(TAG, "download canceled during stream")
                    throw ChatError.Canceled
                }

                val bytesRead = input.read(buffer)
                if (bytesRead < 0) {
                    break
                }

                listener.onChunk(
                    buffer.copyOf(bytesRead)
                )

                downloadedBytes += bytesRead

                logger.debug(
                    TAG,
                    "downloaded: $downloadedBytes/$totalBytes"
                )
            }
        }

        listener.onCompleted(
            DownloadResult(
                bytesDownloaded = downloadedBytes
            )
        )
    }


    private val downloadClient: OkHttpClient by lazy {
        httpClient.newBuilder()
            .callTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }


    private fun Throwable.toChatError(): ChatError =
        when (this) {
            is ChatError -> this
            else -> {
                ChatError.fromCode(
                    ChatError.UNKNOWN_CODE,
                    message ?: toString(),
                    this
                )
            }
        }
}


internal class TransferTaskImpl : Cancellable {
    @Volatile
    private var canceled = false

    @Volatile
    var call: Call? = null

    override fun cancel() {
        if (canceled) {
            return
        }

        canceled = true
        call?.cancel()
    }

    fun isCanceled(): Boolean = canceled
}