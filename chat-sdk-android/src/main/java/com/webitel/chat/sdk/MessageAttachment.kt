package com.webitel.chat.sdk


data class MessageAttachment(
    /** Remote file identifier */
    val fileId: String,

    /** Original file name */
    val fileName: String,

    /** MIME type */
    val mimeType: String,

    /** File size in bytes*/
    val size: Long,

    /** Direct download url if provided */
    val url: String? = null
) {

    val type: Type
        get() = when {
            mimeType.startsWith("image/") -> Type.IMAGE
            mimeType.startsWith("video/") -> Type.VIDEO
            mimeType.startsWith("audio/") -> Type.AUDIO
            else -> Type.FILE
        }

    enum class Type {
        IMAGE,
        VIDEO,
        AUDIO,
        FILE
    }

    val isImage: Boolean get() = type == Type.IMAGE
    val isVideo: Boolean get() = type == Type.VIDEO
    val isAudio: Boolean get() = type == Type.AUDIO
}