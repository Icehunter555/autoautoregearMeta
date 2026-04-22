package dev.wizard.meta.util.pause

import dev.wizard.meta.module.AbstractModule

interface IPause {
    fun requestPause(module: AbstractModule): Boolean
}
