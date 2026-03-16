package com.webitel.chat.sdk


/**
 * Represents a file attachment referenced by a message.
 *
 * Attachments contain metadata only.
 * Binary content must be downloaded explicitly using the provided fileId.
 *
 * Example:
 * ```
 * when (attachment) {
 *   is MessageAttachment.Image -> showImagePreview(attachment)
 *   is MessageAttachment.Audio -> showAudioPlayer(attachment)
 *   else -> showDownloadButton(attachment)
 * }
 * ```
 */
sealed class MessageAttachment {

    /** Remote file identifier used for downloading */
    abstract val fileId: String

    /** Original file name as provided by the sender */
    abstract val fileName: String

    /** MIME type reported by the backend */
    abstract val mimeType: String

    /** File size in bytes */
    abstract val size: Long


    /**
     * Image attachment.
     *
     * Contains image metadata and optional preview information.
     * The full image content must be downloaded explicitly.
     */
    data class Image(
        override val fileId: String,
        override val fileName: String,
        override val mimeType: String,
        override val size: Long,

        /** Optional preview (thumbnail) file identifier */
        val previewId: String?,

        /** Image width in pixels, if known */
        val width: Int?,

        /** Image height in pixels, if known */
        val height: Int?
    ) : MessageAttachment()


    /**
     * Video attachment.
     *
     * Represents a video file with optional metadata useful for UI rendering.
     */
    data class Video(
        override val fileId: String,
        override val fileName: String,
        override val mimeType: String,
        override val size: Long,

        /** Duration in milliseconds, if provided by backend */
        val durationMs: Long?,

        /** Codec information, if available */
        val codec: String?,

        /** Optional preview frame file identifier */
        val previewId: String?
    ) : MessageAttachment()


    /**
     * Audio attachment.
     *
     * Used for voice messages or audio files.
     */
    data class Audio(
        override val fileId: String,
        override val fileName: String,
        override val mimeType: String,
        override val size: Long,

        /** Duration in milliseconds, if known */
        val durationMs: Long?,

        /** Codec information, if available */
        val codec: String?
    ) : MessageAttachment()


    /**
     * Document attachment.
     *
     * Typically used for files such as PDF, DOCX, XLSX, etc.
     */
    data class Document(
        override val fileId: String,
        override val fileName: String,
        override val mimeType: String,
        override val size: Long,

        /** Number of pages, if provided by backend */
        val pageCount: Int?
    ) : MessageAttachment()


    /**
     * Generic file attachment.
     *
     * Fallback type for files that do not fit into other categories
     * or do not provide enough metadata for specialized handling.
     */
    data class File(
        override val fileId: String,
        override val fileName: String,
        override val mimeType: String,
        override val size: Long
    ) : MessageAttachment()
}