package dev.wizard.meta.command.execute

import dev.wizard.meta.command.AbstractCommandManager
import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.ArgIdentifier

interface IExecuteEvent {
    val commandManager: AbstractCommandManager<*>
    val args: Array<String>

    suspend fun mapArgs(argTree: List<AbstractArg<*>>)

    fun <T : Any> getValue(identifier: ArgIdentifier<T>): T
}
