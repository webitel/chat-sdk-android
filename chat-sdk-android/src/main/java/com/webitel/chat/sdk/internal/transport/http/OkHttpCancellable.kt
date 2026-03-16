package com.webitel.chat.sdk.internal.transport.http

import com.webitel.chat.sdk.Cancellable
import okhttp3.Call

internal class OkHttpCancellable(
    private val call: Call
) : Cancellable {
    override fun cancel() {
        call.cancel()
    }
}