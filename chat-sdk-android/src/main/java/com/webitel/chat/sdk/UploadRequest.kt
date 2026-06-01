package com.webitel.chat.sdk

import java.io.InputStream


/**
 * Parameters required to upload a file.
 */
data class UploadRequest(

    /**
     * File content source.
     */
    val source: FileSource,

    /**
     * Original file name.
     */
    val fileName: String,

    /**
     * MIME type of the file.
     *
     * Example:
     * `image/png`, `application/pdf`
     */
    val mimeType: String,

    /**
     * Total file size in bytes, if known.
     *
     * Providing the size allows more accurate
     * progress reporting and resumable uploads.
     */
    val totalSize: Long? = null,

    /**
     * Existing upload identifier used to resume
     * a previously interrupted upload session.
     */
    val resumeId: String? = null
)


/**
 * Represents supported upload content sources.
 */
sealed interface FileSource {

    /**
     * File content provided as an [InputStream].
     *
     * Useful for streaming large files
     * without loading them fully into memory.
     */
    data class Stream(
        val stream: InputStream
    ) : FileSource

    /**
     * File content stored fully in memory.
     */
    data class Bytes(
        val bytes: ByteArray
    ) : FileSource
}


/**
 * Represents the result of a completed upload operation.
 */
data class UploadResult(
    /**
     * Metadata of the uploaded file.
     */
    val file: UploadedFile,

    /**
     * Hashsum values calculated for the uploaded content.
     * The key represents the hash algorithm name
     * and the value contains the corresponding hash.
     *
     * Example:
     * `sha256 -> abc123`
     */
    val hash: Map<String, String>
)


/**
 * Metadata describing an uploaded file.
 */
data class UploadedFile(

    /**
     * Unique remote file identifier.
     */
    val id: String,

    /**
     * Original file name.
     */
    val name: String,

    /**
     * MIME type reported by the backend.
     */
    val mimeType: String,

    /**
     * File size in bytes.
     */
    val size: Long
)


interface UploadListener {

    /**
     * Called when a new upload session is created.
     *
     * The upload identifier can later be used
     * to resume interrupted uploads.
     */
    fun onCreated(uploadId: String)

    /**
     * Called when upload progress changes.
     *
     * @param uploaded Number of bytes uploaded so far.
     * @param total Total file size in bytes, if known.
     */
    fun onProgress(
        uploaded: Long,
        total: Long?
    )

    /**
     * Called when the upload completes successfully.
     */
    fun onCompleted(result: UploadResult)

    /**
     * Called when the upload fails or is canceled.
     *
     * If the transfer was canceled explicitly,
     * the error will be [ChatError.Canceled].
     */
    fun onError(error: ChatError)
}