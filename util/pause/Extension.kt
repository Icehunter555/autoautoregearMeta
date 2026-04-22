package dev.wizard.meta.util.pause

import dev.wizard.meta.module.AbstractModule

inline fun <R> IPause.withPause(module: AbstractModule, block: () -> R): R? {
    synchronized(this) {
        return if (requestPause(module)) block() else null
    }
}

inline fun <R> PriorityTimeoutPause.withPause(module: AbstractModule, timeout: Int, block: () -> R): R? {
    return withPause(module, timeout.toLong(), block)
}

inline fun <R> PriorityTimeoutPause.withPause(module: AbstractModule, timeout: Long, block: () -> R): R? {
    synchronized(this) {
        return if (requestPause(module, timeout)) block() else null
    }
}
