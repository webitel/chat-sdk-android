package com.webitel.chat.sdk


interface DownloadListener {
    /**
     * Called when a new chunk of file data is received.
     *
     * The chunk contains only valid downloaded bytes and can be processed,
     * stored, or forwarded immediately.
     */
    fun onChunk(chunk: ByteArray)

    /**
     * Called when the download completes successfully.
     *
     * Provides final transfer statistics.
     */
    fun onCompleted(result: DownloadResult)

    /**
     * Called when the download fails or is canceled.
     *
     * If the transfer was canceled explicitly,
     * the error will be [ChatError.Canceled].
     */
    fun onError(error: ChatError)
}


/**
 * Parameters used to start a file download.
 */
data class DownloadRequest(

    /**
     * Remote file identifier.
     */
    val fileId: String,

    /**
     * Byte offset to start downloading from.
     *
     * Can be used to resume interrupted downloads.
     */
    val offset: Long = 0
)


/**
 * Represents the result of a completed download operation.
 */
data class DownloadResult(

    /**
     * Total number of bytes downloaded.
     */
    val bytesDownloaded: Long
)