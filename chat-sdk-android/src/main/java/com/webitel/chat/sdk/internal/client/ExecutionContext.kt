package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal class ExecutionContext {
    private val apiExecutor = Executors.newFixedThreadPool(
        5,
        named("chat-sdk-api")
    )

    private val transferExecutor = Executors.newFixedThreadPool(
        5,
        named("chat-sdk-transfer")
    )

    private val realtimeExecutor = Executors.newSingleThreadExecutor(
        named("chat-sdk-realtime")
    )

    fun api(task: () -> Unit) = apiExecutor.execute(task)
    fun transfer(task: () -> Unit) = transferExecutor.execute(task)
    fun realtime(task: () -> Unit) = realtimeExecutor.execute(task)

    fun shutdown() {
        apiExecutor.shutdown()
        transferExecutor.shutdown()
        realtimeExecutor.shutdown()
    }

    private fun named(name: String) = ThreadFactory { runnable ->
        Thread(runnable, name).apply {
            isDaemon = true
            uncaughtExceptionHandler =
                Thread.UncaughtExceptionHandler { _, e ->
                    logger.error("ExecutionContext","Uncaught exception in $name; Exception $e")
                }
        }
    }
}