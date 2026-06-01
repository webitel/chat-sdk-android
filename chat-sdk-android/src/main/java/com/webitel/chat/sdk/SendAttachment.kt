package com.webitel.chat.sdk


/**
 * Represents an attachment reference used in outgoing messages.
 */
sealed class SendAttachment {

    /**
     * Reference to a previously uploaded file.
     *
     * The file must already exist on the remote storage.
     */
    data class File(

        /**
         * Unique remote file identifier.
         */
        val fileId: String
    ) : SendAttachment()

    /**
     * Reference to an external file URL.
     *
     * The backend may fetch, validate,
     * or proxy the remote resource.
     */
    data class Url(

        /**
         * Publicly accessible file URL.
         */
        val url: String,

        /**
         * Original file name including extension.
         */
        val fileName: String
    ) : SendAttachment()
}