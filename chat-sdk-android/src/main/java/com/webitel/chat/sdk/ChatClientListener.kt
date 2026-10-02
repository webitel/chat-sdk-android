package com.webitel.chat.sdk


/**
 * Listener for client-level events not bound to a specific dialog.
 *
 * All methods have default empty implementations,
 * override only those you need.
 */
interface ChatClientListener {

    /**
     * Called when incremental synchronization is no longer possible
     * and the client should reload its chat state:
     * the dialog list and any opened message histories.
     */
    fun onResyncRequired() {}
}
