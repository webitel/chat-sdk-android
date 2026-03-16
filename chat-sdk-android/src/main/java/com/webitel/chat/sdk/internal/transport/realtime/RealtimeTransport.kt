package com.webitel.chat.sdk.internal.transport.realtime

import com.webitel.chat.sdk.ConnectionState

internal interface RealtimeTransport {

    fun connect()
    fun disconnect()

    fun setListener(listener: RealtimeListener)

    val connectionState: ConnectionState
}