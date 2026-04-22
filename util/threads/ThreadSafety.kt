package dev.wizard.meta.util.threads

import dev.wizard.meta.event.ClientExecuteEvent
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.SafeExecuteEvent
import kotlinx.coroutines.CompletableDeferred

fun ClientExecuteEvent.toSafe(): SafeExecuteEvent? {
    val world = world ?: return null
    val player = player ?: return null
    val playerController = playerController ?: return null
    val connection = connection ?: return null
    return SafeExecuteEvent(world, player, playerController, connection, this)
}

inline fun <R> runSafeOrElse(defaultValue: R, block: (SafeClientEvent) -> R): R {
    return SafeClientEvent.instance?.let { block(it) } ?: defaultValue
}

inline fun runSafeOrFalse(block: (SafeClientEvent) -> Boolean): Boolean {
    return SafeClientEvent.instance?.let { block(it) } ?: false
}

inline fun <R> runSafe(block: (SafeClientEvent) -> R?): R? {
    return SafeClientEvent.instance?.let { block(it) }
}

inline fun runTrying(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
    }
}

suspend fun <R> runSafeSuspend(block: suspend (SafeClientEvent) -> R): R? {
    return SafeClientEvent.instance?.let { block(it) }
}

fun <T> onMainThreadSafe(block: (SafeClientEvent) -> T): CompletableDeferred<T?> {
    return onMainThread {
        SafeClientEvent.instance?.let { block(it) }
    }
}

fun <T> onMainThread(block: () -> T): CompletableDeferred<T> {
    return MainThreadExecutor.add(block)
}

inline fun <T : Any, R> T.runSynchronized(block: (T) -> R): R {
    synchronized(this) {
        return block(this)
    }
}
