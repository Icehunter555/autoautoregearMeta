package dev.wizard.meta.util.pause

import dev.wizard.meta.module.AbstractModule

interface ITimeoutPause : IPause {
    override fun requestPause(module: AbstractModule): Boolean {
        return requestPause(module, 50L)
    }

    fun requestPause(module: AbstractModule, timeout: Int): Boolean {
        return requestPause(module, timeout.toLong())
    }

    fun requestPause(module: AbstractModule, timeout: Long): Boolean
}
