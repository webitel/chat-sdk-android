package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.Cancellable
import com.webitel.chat.sdk.ChatClient
import com.webitel.chat.sdk.ChatError
import com.webitel.chat.sdk.ChatEventListener
import com.webitel.chat.sdk.ConnectionListener
import com.webitel.chat.sdk.ConnectionState
import com.webitel.chat.sdk.Contact
import com.webitel.chat.sdk.ContactRequest
import com.webitel.chat.sdk.Dialog
import com.webitel.chat.sdk.DialogEvent
import com.webitel.chat.sdk.DialogRequest
import com.webitel.chat.sdk.DownloadListener
import com.webitel.chat.sdk.HistoryRequest
import com.webitel.chat.sdk.HistorySlice
import com.webitel.chat.sdk.Message
import com.webitel.chat.sdk.MessageEvent
import com.webitel.chat.sdk.MessageOptions
import com.webitel.chat.sdk.MessageTarget
import com.webitel.chat.sdk.Page
import com.webitel.chat.sdk.internal.api.ChatApiDelegate
import com.webitel.chat.sdk.internal.auth.AuthManager
import com.webitel.chat.sdk.internal.extensions.toDomain
import com.webitel.chat.sdk.internal.transport.dto.DialogDto
import com.webitel.chat.sdk.internal.transport.dto.MessageDto
import com.webitel.chat.sdk.internal.transport.realtime.RealtimeListener
import com.webitel.chat.sdk.internal.transport.realtime.RealtimeTransport
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.math.pow

internal class ChatClientImpl(
    private val clientContext: ClientContext,
    private val api: ChatApiDelegate,
    private val authManager: AuthManager,
    private val realtime: RealtimeTransport,
    private val hub: RealtimeHub): ChatClient {
    private var retryAttempt = 0
    private var realtimeEnabled = false
    private var backoffTask: TimerTask? = null

    private val dialogFactory = DialogFactory(
        client = this,
        realtimeHub = hub
    )

    override val connectionState: ConnectionState
        get() {
            // 1. If a backoff timer is active, we are currently in the reconnection process
            if (backoffTask != null) return ConnectionState.Connecting

            // 2. If no backoff is active, return the actual state from the realtime provider
            return realtime.connectionState
        }

    val currentUserId: String?
        get() {
            return authManager.currentContact?.id
        }


    companion object {
        val TAG = "ChatClientImpl"
        val logger: WLogger = WLogger()
    }


    init {
        logger.level = clientContext.logLevel
        realtime.setListener(realtimeListener())
    }


    override fun sendMessage(
        target: MessageTarget,
        options: MessageOptions,
        onComplete: (Result<String>) -> Unit
    ): Cancellable {
        return callCancellableWithAuthRetry(
            call = { callback ->
                api.sendMessage(target, options, callback)
            },
            onComplete = onComplete
        )
    }


    override fun getDialogs(request: DialogRequest, onComplete: (Result<Page<Dialog>>) -> Unit) {
        callWithAuthRetry(
            call = { callback ->
                api.getDialogs(request) { result ->
                    callback(
                        result.map { page ->
                            Page(
                                page.page,
                                page.items.map { dialogFactory.getOrCreate(it) },
                                page.hasNext
                            )
                        }
                    )
                }
            },
            onComplete = onComplete
        )
    }


    override fun getContacts(request: ContactRequest, onComplete: (Result<Page<Contact>>) -> Unit) {
        callWithAuthRetry(
            call = { callback ->
                api.getContacts(request) { result ->
                    callback(
                        result.map { page ->
                            Page(
                                page.page,
                                page.items.map { it.toDomain() },
                                page.hasNext
                            )
                        }
                    )
                }
            },
            onComplete = onComplete
        )
    }


    override fun registerDevice(
        pushToken: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        callWithAuthRetry(
            call = { callback ->
                api.registerDevice(pushToken, callback)
            },
            onComplete = onComplete
        )
    }


    override fun endSession(onComplete: (Result<Unit>) -> Unit) {
        disconnect()
        authManager.endSession(onComplete)
    }


    override fun connect() {
        logger.debug(TAG, "called connect()")
        if (realtimeEnabled) {
            logger.debug(TAG,
                "connect: realtime is enabled. State $connectionState"
            )
            return
        }

        realtimeEnabled = true
        retryAttempt = 0
        hub.updateState(ConnectionState.Connecting)
        realtime.connect()
    }


    override fun disconnect() {
        realtimeEnabled = false
        backoffTask?.cancel()
        backoffTask = null
        retryAttempt = 0
        realtime.disconnect()
    }


    override fun addEventListener(listener: ChatEventListener) {
        hub.addGlobalListener(listener)
    }


    override fun removeEventListener(listener: ChatEventListener) {
        hub.removeGlobalListener(listener)
    }


    override fun addConnectionListener(listener: ConnectionListener) {
        hub.addConnectionListener(listener)
    }


    override fun removeConnectionListener(listener: ConnectionListener) {
        hub.removeConnectionListener(listener)
    }


    fun getHistory(
        dialogId: String,
        request: HistoryRequest,
        onComplete: (Result<HistorySlice<Message>>) -> Unit
    ) {
        return callWithAuthRetry(
            call = { callback ->
                api.getHistory(dialogId, request) { resultDto ->

                    val mapped = resultDto.map { slice ->
                        val currentUserId = authManager.currentContact?.id
                        HistorySlice(
                            items = slice.items.map { it.toDomain(currentUserId) },
                            newerCursor = slice.newerCursor,
                            olderCursor = slice.olderCursor
                        )
                    }
                    callback(mapped)
                }
            },
            onComplete = onComplete
        )
    }


    fun downloadFile(
        dialogId: String,
        fileId: String,
        offset: Long?,
        listener: DownloadListener
    ): Cancellable {

        var retried = false
        var currentCall: Cancellable?

        fun startDownload(): Cancellable {
            return api.downloadFile(
                dialogId,
                fileId,
                offset,
                object : DownloadListener {

                    override fun onChunk(bytes: ByteArray) {
                        listener.onChunk(bytes)
                    }

                    override fun onComplete() {
                        listener.onComplete()
                    }

                    override fun onCanceled() {
                        listener.onCanceled()
                    }

                    override fun onError(error: ChatError) {
                        if (error is ChatError.Unauthorized && !retried) {
                            retried = true

                            authManager.refresh { authResult ->
                                if (authResult.isSuccess) {
                                    currentCall = startDownload()
                                } else {
                                    listener.onError(authResult.exceptionOrNull() as ChatError)
                                }
                            }
                        } else {
                            listener.onError(error)
                        }
                    }
                }
            )
        }

        currentCall = startDownload()

        return object : Cancellable {
            override fun cancel() {
                currentCall?.cancel()
            }
        }
    }


    private fun realtimeListener(): RealtimeListener =
        object : RealtimeListener {
            override fun onMessage(message: MessageDto){
                val dialog = dialogFactory.get(message.dialogId)
                val messageDomain = message.toDomain(authManager.currentContact?.id)
                dialog?.applyMessage(message)

                hub.dispatch(
                    MessageEvent.Received(message.dialogId, messageDomain)
                )
            }

            override fun onNewDialog(dialog: DialogDto) {
                val newDialog = dialogFactory.getOrCreate(dialog)
                hub.dispatch(
                    DialogEvent.Created(newDialog.id, newDialog)
                )
            }

            override fun onError(error: ChatError) {
                if (!canRetry(retryAttempt)) {
                    failRealtime(error)
                    return
                }

                if (error is ChatError.Unauthorized) {
                    refreshAuthAndReconnect()
                    return
                }

                tryConnect()
            }

            override fun onOpen() {
                retryAttempt = 0
                hub.updateState(
                    ConnectionState.Connected
                )
            }

            override fun onClosed(code: Int, reason: String) {
                if (!realtimeEnabled) {
                    hub.updateState(ConnectionState.Disconnected)
                    return
                }

                if (!canRetry(retryAttempt)) {
                    closeRealtime(code, reason)
                    return
                }

                if (code == 401 || code == 1008) {
                    refreshAuthAndReconnect()
                    return
                }

                tryConnect()
            }
        }


    private fun failRealtime(error: ChatError) {
        realtimeEnabled = false

        logger.error(
            "ChatClientImpl",
            "connect: Realtime connection failed. $error"
        )

        hub.updateState(ConnectionState.Failed(error))
    }


    private fun refreshAuthAndReconnect() {
        authManager.refresh { authResult ->
            if (!realtimeEnabled) return@refresh

            if (authResult.isSuccess) {
                tryConnect()
            } else {
                failRealtime(authResult.exceptionOrNull() as ChatError)
            }
        }
    }


    private fun closeRealtime(code: Int, reason: String) {
        logger.error(TAG, "onClosed: close realtime")

        realtimeEnabled = false
        hub.updateState(
            ConnectionState.Failed(
                ChatError.fromCode(code, reason)
            )
        )
    }


    private fun tryConnect() {
        if (backoffTask != null) {
            logger.debug("ChatClientImpl",
                "connect: backoffTask is active"
            )
            return
        }

        retryAttempt++
        logger.debug("ChatClientImpl",
            "connect: retry open connection. Attempt $retryAttempt"
        )

        hub.updateState(
            ConnectionState.Reconnecting(retryAttempt,
                clientContext.networkConfig.realtime.maxRetries
            )
        )

        val delay = calculateBackoff(retryAttempt)
        logger.debug("ChatClientImpl",
            "connect: calculated backoff delay $delay"
        )
        backoffTask = Timer().schedule(delay) {
            backoffTask = null
            realtime.connect()
        }
    }


    private fun calculateBackoff(attempt: Int): Long =
        (clientContext.networkConfig.realtime.retryBaseDelayMs * 2.0.pow(attempt.toDouble()).toLong())
            .coerceAtMost(clientContext.networkConfig.realtime.maxRetryDelayMs)


    private fun canRetry(attempt: Int): Boolean {
        return realtimeEnabled &&
                attempt < clientContext.networkConfig.realtime.maxRetries
    }


    private fun <T> callWithAuthRetry(
        call: (onComplete: (Result<T>) -> Unit) -> Unit,
        onComplete: (Result<T>) -> Unit
    ) {
        var retried = false

        fun execute() {
            call { result ->
                result.fold(
                    onSuccess = { onComplete(result) },
                    onFailure = { error ->
                        if (error is ChatError.Unauthorized && !retried) {
                            retried = true

                            authManager.refresh { authResult ->
                                if (authResult.isSuccess) {
                                    execute()
                                } else {
                                    onComplete(Result.failure(authResult.exceptionOrNull()!!))
                                }
                            }
                        } else {
                            onComplete(Result.failure(error))
                        }
                    }
                )
            }
        }

        execute()
    }


    private fun <T> callCancellableWithAuthRetry(
        call: (onComplete: (Result<T>) -> Unit) -> Cancellable,
        onComplete: (Result<T>) -> Unit
    ): Cancellable {

        val composite = CompositeCancellable()
        var retried = false

        fun executeCall() {
            if (composite.isCanceled()) return

            val c = call { result ->
                if (composite.isCanceled()) return@call

                result.fold(
                    onSuccess = { onComplete(Result.success(it)) },
                    onFailure = { error ->
                        if (error is ChatError.Unauthorized && !retried) {
                            retried = true

                            authManager.refresh { authResult ->
                                if (composite.isCanceled()) return@refresh

                                if (authResult.isSuccess) {
                                    executeCall()
                                } else {
                                    onComplete(Result.failure(authResult.exceptionOrNull()!!))
                                }
                            }
                        } else {
                            onComplete(Result.failure(error))
                        }
                    }
                )
            }

            composite.add(c)
        }

        executeCall()
        return composite
    }
}