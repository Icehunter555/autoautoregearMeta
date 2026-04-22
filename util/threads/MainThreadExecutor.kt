package dev.wizard.meta.util.threads

import dev.fastmc.common.DoubleBuffered
import dev.fastmc.common.collection.FastObjectArrayList
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.util.Wrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith

object MainThreadExecutor : AlwaysListening {
    private val jobs = DoubleBuffered { FastObjectArrayList<MainThreadJob<*>>() }
    private val signal = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    private fun runJobs() {
        signal.tryReceive()
        val front = jobs.swap().front
        synchronized(front) {
            front.forEach { it.run() }
            front.clear()
        }
    }

    suspend fun runJobAdapter() {
        signal.receive()
        val front = jobs.swap().front
        synchronized(front) {
            front.forEach { it.run() }
            front.clear()
        }
    }

    fun <T> add(block: () -> T): CompletableDeferred<T> {
        val job = MainThreadJob(block)
        if (Wrapper.minecraft.isCallingFromMinecraftThread) {
            job.run()
        } else {
            val back = jobs.back
            synchronized(back) {
                back.add(job)
            }
            signal.trySend(Unit)
        }
        return job.deferred
    }

    init {
        val priority = Int.MAX_VALUE
        listener<RunGameLoopEvent.Start>(priority = priority, alwaysListening = true) { runJobs() }
        listener<RunGameLoopEvent.Tick>(priority = priority, alwaysListening = true) { runJobs() }
        listener<RunGameLoopEvent.Render>(priority = priority, alwaysListening = true) { runJobs() }
        listener<RunGameLoopEvent.End>(priority = priority, alwaysListening = true) { runJobs() }
    }

    private class MainThreadJob<T>(private val block: () -> T) {
        val deferred = CompletableDeferred<T>()

        fun run() {
            deferred.completeWith(runCatching { block() })
        }
    }
}
