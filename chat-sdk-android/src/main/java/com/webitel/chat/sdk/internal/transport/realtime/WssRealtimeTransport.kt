package com.webitel.chat.sdk.internal.transport.realtime

import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.ConnectionState
import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import com.webitel.chat.sdk.internal.client.ClientContext
import com.webitel.chat.sdk.internal.client.ExecutionContext
import com.webitel.chat.sdk.internal.transport.http.HeaderInterceptor
import com.webitel.chat.sdk.internal.transport.http.HeaderProvider
import com.webitel.chat.sdk.internal.transport.parser.Parser
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal class WssRealtimeTransport(
    private val clientContext: ClientContext,
    private val execution: ExecutionContext,
    private val headerProvider: HeaderProvider
):  WebSocketListener(), RealtimeTransport {
    override var connectionState: ConnectionState = ConnectionState.Disconnected
        private set

    private var realtimeListener: RealtimeListener? = null
    private var socket: WebSocket? = null

    private val parser = Parser()

    val client: OkHttpClient

    init {
        val clientBuilder = OkHttpClient.Builder()
            .pingInterval(clientContext.networkConfig.realtime.pingIntervalMs,
                TimeUnit.MILLISECONDS)
            .callTimeout(clientContext.networkConfig.api.callTimeoutMs,
                TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(HeaderInterceptor(headerProvider))

        applyCertificatePinning(clientBuilder)

        client = clientBuilder.build()
    }

    private companion object {
        const val TAG = "WssRealtimeTransport"
        const val WS_PATH = "/im/ws"
    }


    override fun connect() {
        execution.realtime {
            logger.debug(TAG, "connect()")
            openStream()
        }
    }


    override fun disconnect() {
        execution.realtime {
            logger.debug(TAG, "disconnect()")
            closeStream()
        }
    }


    override fun setListener(listener: RealtimeListener) {
        realtimeListener = listener
    }


    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.debug(TAG, "onOpen: Connected to WebSocket server")
        connectionState = ConnectionState.Connected
        realtimeListener?.onOpen()
    }


    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logger.warn(TAG, "onClosed: $code, $reason")
        connectionState = ConnectionState.Disconnected
        realtimeListener?.onClosed(code, reason)
    }


    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.warn(TAG, "onFailure: ${t}, $response")

        val httpCode = response?.code
        val httpBody = response?.body?.let {
            try {
                it.string()
            } catch (_: Exception) {
                null
            }
        }

        val errorMessage = when {
            httpCode != null -> "WebSocket HTTP error: $httpCode $httpBody"
            t.message != null -> "WebSocket error: ${t.message}"
            else -> "Unknown WebSocket error"
        }

        val error = ChatError.Companion.fromCode(
            httpCode ?: ChatError.Companion.UNKNOWN_CODE,
            errorMessage,
            t
        )

        connectionState = ConnectionState.Failed(error)
        logger.debug(
            TAG,
            "onFailure: ConnectionState - ${connectionState}"
        )

        realtimeListener?.onError(error)
    }


    override fun onMessage(webSocket: WebSocket, text: String) {
        val json = JSONObject(text)
        logger.debug(TAG, "onMessage: received $text;")

        val payload = json.optJSONObject("payload")
        if (payload == null) {
            logger.error(TAG, "onMessage: payload not found in $text")
            return
        }

        val event = EventType.entries.find { payload.has(it.value) }
        if (event == null){
            logger.error(TAG, "onMessage: event not found in $text;")
            return
        }

        when (event) {
            EventType.Connected -> handleConnected(payload)
            EventType.Disconnected -> handleDisconnected(payload)
            EventType.Message -> handleMessage(payload)
            EventType.Ack -> handleAck(payload)
            EventType.Error -> handleError(payload)
            EventType.Ping -> handlePing(payload)
            EventType.Unsupported -> handleUnsupported(payload)
            EventType.DialogCreated -> handleDialogCreated(payload)
        }
    }


    fun onAuthUpdated(token: String) {
        if (connectionState == ConnectionState.Connected) {
            logger.debug(TAG, "send new auth in stream; $token")
            socket?.send(JSONObject()
                .put("x-webitel-access", token)
                .put("x-webitel-client", clientContext.clientToken)
                .toString()
            )
        }
    }


    private fun handleConnected(payload: JSONObject) {
        logger.debug(TAG, "handleConnected: $payload")
    }


    private fun handleDisconnected(payload: JSONObject) {
        logger.debug(TAG, "handleDisconnected: $payload")
        val disconnectObj = payload.optJSONObject("disconnected_event")

        val reason = disconnectObj?.optString("reason") ?: "unknown"
        val code = disconnectObj?.optInt("code") ?: 1000
        closeStream(reason, code)
    }


    private fun closeStream(reason: String, code: Int) {
        try {
            socket?.close(
                safeCloseCode(code),
                reason
            )
        } catch (_: Exception) { }
    }


    private fun safeCloseCode(code: Int): Int =
        when (code) {
            401 -> 1008
            else -> 1000
        }


    private fun handleMessage(payload: JSONObject) {
        logger.debug(TAG, "handleMessage: $payload")

        val messageObj = payload.optJSONObject("message_event")
        val message = parser.parseMessage(messageObj)
        if (message == null) {
            logger.warn(TAG, "handleMessage: Invalid message payload, ignored")
            return
        }

        realtimeListener?.onMessage(message)
    }
    

    private fun handleError(payload: JSONObject) {
        logger.debug(TAG, "handleError: $payload")
    }


    private fun handleAck(payload: JSONObject) {
        logger.debug(TAG, "handleAck: $payload")
    }


    private fun handlePing(payload: JSONObject) {
        logger.debug(TAG, "handlePing: $payload")
    }


    private fun handleDialogCreated(payload: JSONObject) {
        logger.debug(TAG, "handleDialogCreated: $payload")

        val dialogObj = payload.optJSONObject("thread_created_event")
        val dialog = parser.parseDialog(dialogObj)
        if (dialog == null) {
            logger.warn(TAG, "handleDialogCreated: Invalid dialog payload, ignored")
            return
        }

        realtimeListener?.onNewDialog(dialog)
    }


    private fun handleUnsupported(payload: JSONObject) {
        logger.debug(TAG, "handleUnsupported: $payload")
    }


    private fun openStream() {
        when (connectionState) {
            ConnectionState.Connected,
            ConnectionState.Connecting,
            is ConnectionState.Reconnecting -> {
                logger.warn(TAG, "openStream: stream is already open or connecting - $connectionState")
                return
            }

            else -> {}
        }

        connectionState = ConnectionState.Connecting
        val request = buildWebSocketRequest()
        logger.debug(TAG, "open stream - $request")
        socket = client.newWebSocket(request, this)
    }


    private fun buildWebSocketRequest(): Request {
        val portPart = if (clientContext.port > 0) ":${clientContext.port}" else ""
        val url = "${clientContext.scheme}://${clientContext.host}$portPart${WS_PATH}"

        val builder = Request.Builder()
            .url(url)

        headerProvider.commonHeaders().forEach { (k, v) ->
            builder.addHeader(k, v)
        }

        return builder.build()
    }


    private fun closeStream() {
        try {
            socket?.close(1000, "Client closed")
        } catch (e: Exception) {
            logger.error(TAG, "closeConnection: error" + e.message.toString())
        }
    }


    private fun applyCertificatePinning(builder: OkHttpClient.Builder) {
        if (clientContext.pinnedHashes.isEmpty()) return

        val normalizedPins = clientContext.pinnedHashes
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { normalizeSha256Pin(it) }
            .toList()

        if (normalizedPins.isEmpty()) return

        val certificatePinner = CertificatePinner.Builder()
            .add(clientContext.host, *normalizedPins.toTypedArray())
            .build()

        builder.certificatePinner(certificatePinner)
    }


    private fun normalizeSha256Pin(pin: String): String {
        return if (pin.startsWith("sha256/")) {
            pin
        } else {
            "sha256/$pin"
        }
    }
}