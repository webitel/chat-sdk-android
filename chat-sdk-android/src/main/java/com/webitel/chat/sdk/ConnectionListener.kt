package com.webitel.chat.sdk


/**
 * Listener used to observe realtime connection state changes.
 */
interface ConnectionListener {

    /** Called whenever the connection state changes. */
    fun onStateChanged(state: ConnectionState)
}