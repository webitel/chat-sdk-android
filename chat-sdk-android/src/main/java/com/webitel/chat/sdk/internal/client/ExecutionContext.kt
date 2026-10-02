package com.webitel.chat.sdk.internal.client

import com.webitel.chat.sdk.internal.client.ChatClientImpl.Companion.logger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

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

    /** Serial queue of the updates synchronizer: realtime events are processed and dispatched here. */
    private val syncExecutor = Executors.newSingleThreadScheduledExecutor(
        named("chat-sdk-sync")
    )

    fun api(task: () -> Unit) = apiExecutor.execute(task)
    fun transfer(task: () -> Unit) = transferExecutor.execute(task)
    fun realtime(task: () -> Unit) = realtimeExecutor.execute(task)
    // A scheduled executor wraps every task (`execute` included) into a Future,
    // which swallows exceptions silently: log them instead
    fun sync(task: () -> Unit) = syncExecutor.execute(logged("sync", task))

    fun syncDelayed(delayMs: Long, task: () -> Unit) {
        syncExecutor.schedule(logged("delayed sync", task), delayMs, TimeUnit.MILLISECONDS)
    }

    private fun logged(name: String, task: () -> Unit) = Runnable {
        try {
            task()
        } catch (t: Throwable) {
            logger.error("ExecutionContext", "Uncaught exception in $name task; Exception $t")
        }
    }

    fun shutdown() {
        apiExecutor.shutdown()
        transferExecutor.shutdown()
        realtimeExecutor.shutdown()
        syncExecutor.shutdown()
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