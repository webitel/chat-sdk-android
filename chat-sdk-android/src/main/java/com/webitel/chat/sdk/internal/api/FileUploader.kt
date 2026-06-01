package com.webitel.chat.sdk.internal.api

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.UploadListener
import com.webitel.chat.sdk.UploadRequest


internal interface FileUploader {
    fun upload(
        request: UploadRequest,
        listener: UploadListener
    ): Cancellable
}