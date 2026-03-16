package com.webitel.chat.sdk.internal.client

import android.util.Log
import com.webitel.chat.sdk.ChatEvent
import com.webitel.chat.sdk.ChatEventListener
import com.webitel.chat.sdk.ConnectionListener
import com.webitel.chat.sdk.ConnectionState
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

internal class RealtimeHub {
    private val globalListeners = CopyOnWriteArraySet<ChatEventListener>()
    private val dialogListeners =
        ConcurrentHashMap<String, CopyOnWriteArraySet<ChatEventListener>>()
    private val connectionListeners = CopyOnWriteArraySet<ConnectionListener>()

    private var publishedState: ConnectionState = ConnectionState.Disconnected


    fun addGlobalListener(listener: ChatEventListener) {
        globalListeners += listener
    }


    fun addDialogListener(dialogId: String, listener: ChatEventListener) {
        dialogListeners
            .computeIfAbsent(dialogId) { CopyOnWriteArraySet() }
            .add(listener)
    }


    fun removeGlobalListener(listener: ChatEventListener) {
        globalListeners -= listener
    }


    fun removeDialogListener(dialogId: String, listener: ChatEventListener) {
        dialogListeners[dialogId]?.apply {
            remove(listener)
            if (isEmpty()) {
                dialogListeners.remove(dialogId, this)
            }
        }
    }


    fun dispatch(event: ChatEvent) {
        globalListeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (t: Throwable) {
                logClientListenerCrash(
                    scope = "global",
                    event = event::class.simpleName,
                    throwable = t
                )
            }
        }
        dialogListeners[event.dialogId]?.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (t: Throwable) {
                logClientListenerCrash(
                    scope = "dialog:${event.dialogId}",
                    event = event::class.simpleName,
                    throwable = t
                )
            }
        }
    }


    fun addConnectionListener(listener: ConnectionListener) {
        connectionListeners += listener
        listener.onStateChanged(publishedState)
    }


    fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners -= listener
    }


    fun updateState(newState: ConnectionState) {
        if (publishedState == newState) return
       logger.debug("RealtimeHub", "updateState: from $publishedState; to $newState")
        publishedState = newState
        connectionListeners.forEach { listener ->
            try {
                listener.onStateChanged(newState)
            } catch (t: Throwable) {
                logClientListenerCrash(
                    scope = "onStateChanged",
                    event = newState.toString(),
                    throwable = t
                )
            }
        }
    }


    private fun logClientListenerCrash(
        scope: String,
        event: String?,
        throwable: Throwable
    ) {
        Log.e(
            "ChatSDK",
            """
        Client listener crashed
        Scope: $scope
        Event: ${event}
        This exception happened in client code, not in ChatSDK
        """.trimIndent(),
            throwable
        )
    }
}