package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.FileSource
import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.UploadListener
import com.webitel.chat.sdk.UploadRequest
import com.webitel.chat.sdk.UploadResult
import com.webitel.chat.sdk.UploadedFile
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.client.ClientContext
import com.webitel.chat.sdk.internal.client.ExecutionContext
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit


internal class HttpFileUploader(
    private val clientContext: ClientContext,
    private val execution: ExecutionContext,
    private val httpClient: OkHttpClient
) : FileUploader {

    private companion object {
        const val TAG = "HttpFileUploader"
        const val BUFFER_SIZE = 4 * 1024
        const val UPLOAD_PATH = "im/media"
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
    }


    override fun upload(
        request: UploadRequest,
        listener: UploadListener
    ): Cancellable {

        val task = TransferTaskImpl()

        execution.transfer {
            runCatching {

                val uploadId = resolveUploadId(
                    request = request,
                    task = task
                ) { resumeId ->
                    listener.onCreated(resumeId)
                }

                ensureNotCanceled(task)

                uploadFile(
                    request = request,
                    uploadId = uploadId,
                    task = task
                ) { uploaded, total ->
                    listener.onProgress(uploaded, total)
                }

            }.onSuccess {
                listener.onCompleted(it)
            }.onFailure { error ->
                listener.onError(error.toChatError())
            }
        }

        return task
    }


    private fun resolveUploadId(
        request: UploadRequest,
        task: TransferTaskImpl,
        onCreated: ((uploadId: String) -> Unit)?
    ): String {

        return request.resumeId?.also {
            logger.debug(TAG, "resume upload: $it")
        } ?: createUpload(
            request = request,
            task = task,
            onCreated = onCreated
        )
    }


    private fun uploadFile(
        request: UploadRequest,
        uploadId: String,
        task: TransferTaskImpl,
        onProgress: ((uploaded: Long, total: Long?) -> Unit)?
    ): UploadResult {

        val uploadedBytes =
            if (request.resumeId != null) {
                resumeUpload(uploadId, task)
            } else {
                0L
            }

        val requestBody = createUploadBody(
            request = request,
            task = task,
            uploadedBytes = uploadedBytes,
            onProgress = onProgress
        )

        val url = buildUrl(UPLOAD_PATH)
            .addQueryParameter("uploadId", uploadId)
            .build()

        val httpRequest = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        logger.debug(TAG, "upload file: $url")

        val call = uploadClient.newCall(httpRequest)
        task.call = call

        call.execute().use { response ->

            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                logger.error(TAG, "upload failed: ${response.code} $body")
                throw ChatError.fromCode(response.code, body)
            }

            logger.debug(TAG, "upload success: $body")

            return parseUploadResult(body)
        }
    }


    private fun createUploadBody(
        request: UploadRequest,
        task: TransferTaskImpl,
        uploadedBytes: Long,
        onProgress: ((uploaded: Long, total: Long?) -> Unit)?
    ): RequestBody {

        return object : RequestBody() {
            override fun contentType(): MediaType =
                request.mimeType?.toMediaTypeOrNull()
                    ?: DEFAULT_MIME_TYPE.toMediaType()

            override fun writeTo(sink: BufferedSink) {
                request.source.openStream().use { input ->

                    if (uploadedBytes > 0) {
                        input.skipFully(uploadedBytes)
                    }

                    val buffer = ByteArray(BUFFER_SIZE)

                    var uploaded = uploadedBytes

                    while (true) {
                        ensureNotCanceled(task)

                        val read = input.read(buffer)

                        if (read < 0) break

                        sink.write(buffer, 0, read)

                        uploaded += read

                        logger.debug(TAG,
                            "Sent chunk $read; total sent: $uploaded; full size: ${request.totalSize}")

                        onProgress?.invoke(
                            uploaded,
                            request.totalSize
                        )
                    }

                    sink.flush()
                }
            }
        }
    }


    private fun createUpload(
        request: UploadRequest,
        task: TransferTaskImpl,
        onCreated: ((uploadId: String) -> Unit)?
    ): String {
        val payload = JSONObject().apply {
            put("mime_type", request.mimeType)
            put("name", request.fileName)
        }

        val requestBody = payload.toString()
            .toRequestBody("application/json".toMediaType())

        val url = buildUrl(UPLOAD_PATH)
            .build()

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        logger.debug(TAG, "create upload: $payload")

        val call = uploadClient.newCall(httpRequest)
        task.call = call

        call.execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                logger.error(TAG, "create upload failed: ${response.code} $body")
                throw ChatError.fromCode(response.code, body)
            }

            logger.debug(TAG, "create upload success: $body")

            val uploadId = JSONObject(body)
                .getString("uploadId")

            onCreated?.invoke(uploadId)

            return uploadId
        }
    }


    private fun resumeUpload(
        uploadId: String,
        task: TransferTaskImpl
    ): Long {
        val url = buildUrl(UPLOAD_PATH)
            .addQueryParameter("uploadId", uploadId)
            .build()

        val httpRequest = Request.Builder()
            .url(url)
            .get()
            .build()

        logger.debug(TAG, "$url")

        val call = uploadClient.newCall(httpRequest)
        task.call = call

        call.execute().use { response ->

            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                logger.error(TAG, "resume upload failed: ${response.code} $body")
                throw ChatError.fromCode(response.code, body)
            }

            logger.debug(TAG, "resume upload success: $body")

            return JSONObject(body)
                .optLong("size", 0L)
        }
    }


    private fun parseUploadResult(body: String): UploadResult {
        val json = JSONObject(body)

        val file = UploadedFile(
            id = json.getString("fileId"),
            name = json.getString("name"),
            mimeType = json.getString("mimeType"),
            size = json.getLong("size")
        )

        val hash = json.optString("hash")
            .takeIf { it.isNotBlank() }

        return UploadResult(
            file = file,
            hash = hash?.let { mapOf("sha256" to it) }.orEmpty()
        )
    }


    private fun ensureNotCanceled(task: TransferTaskImpl) {
        if (task.isCanceled()) {
            logger.warn(TAG, "upload canceled")
            throw ChatError.Canceled
        }
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


    private val uploadClient: OkHttpClient by lazy {
        httpClient.newBuilder()
            .callTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }


    private fun buildUrl(path: String): HttpUrl.Builder =
        HttpUrl.Builder()
            .scheme(clientContext.scheme)
            .host(clientContext.host)
            .apply {
                if (clientContext.port > 0) {
                    port(clientContext.port)
                }
            }
            .addPathSegments(path)
}


private fun FileSource.openStream(): InputStream =
    when (this) {
        is FileSource.Stream -> stream
        is FileSource.Bytes -> bytes.inputStream()
    }


fun InputStream.skipFully(bytes: Long) {
    var toSkip = bytes
    while (toSkip > 0) {
        val skipped = this.skip(toSkip)
        if (skipped <= 0) break
        toSkip -= skipped
    }
}