package com.webitel.chat.sdk


interface DownloadListener {
    /**
     * Called when data is transferred
     *
     */
    fun onChunk(bytes: ByteArray)

    /**
     * Called when transfer is complete
     *
     */
    fun onComplete()

    /**
     * Called when transfer is canceled
     *
     */
    fun onCanceled()

    /**
     * Called when an error occurred during data transfer
     *
     */
    fun onError(error: ChatError)
}