package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger

internal class CompositeCancellable : Cancellable {
    @Volatile
    private var isCanceled = false

    private val children = mutableListOf<Cancellable>()

    fun add(child: Cancellable) {
        if (isCanceled) {
            child.cancel()
        } else {
            children += child
        }
    }

    override fun cancel() {
        if (isCanceled) {
            logger.warn("CompositeCancellable", "Process is Canceled")
            return
        }
        isCanceled = true
        children.forEach { it.cancel() }
        children.clear()
    }

    fun isCanceled(): Boolean = isCanceled
}