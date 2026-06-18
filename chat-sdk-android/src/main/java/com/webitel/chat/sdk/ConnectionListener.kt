package com.webitel.chat.sdk


/**
 * Listener used to observe realtime connection state changes.
 */
interface ConnectionListener {


    /**
     * Called when the connection state changes.
     *
     * @param from The previous state of the connection.
     * @param to The new state of the connection after the transition.
     */
    fun onStateChanged(from: ConnectionState, to: ConnectionState)
}