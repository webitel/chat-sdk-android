package com.webitel.chat.sdk


interface Cancellable {
    /**
     * Cancels the sending operation.
     *
     * Cancellation does not retract messages already accepted by the server.
     */
    fun cancel()
}
