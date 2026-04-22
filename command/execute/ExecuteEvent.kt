package dev.wizard.meta.command.execute

import dev.wizard.meta.command.AbstractCommandManager
import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.ArgIdentifier
import dev.wizard.meta.command.args.GreedyStringArg

open class ExecuteEvent(
    override val commandManager: AbstractCommandManager<*>,
    override val args: Array<String>
) : IExecuteEvent {

    private val mappedArgs = HashMap<ArgIdentifier<*>, Any>()

    override suspend fun mapArgs(argTree: List<AbstractArg<*>>) {
        val iterator = argTree.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val arg = iterator.next()
            if (arg is GreedyStringArg) {
                val value = arg.convertToType(args.slice(index until args.size).joinToString(" "))
                if (value != null) {
                    mappedArgs[arg.identifier] = value
                }
                return
            } else {
                val value = arg.convertToType(args.getOrNull(index))
                if (value != null) {
                    mappedArgs[arg.identifier] = value
                }
            }
            index++
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(identifier: ArgIdentifier<T>): T {
        return mappedArgs[identifier] as T
    }
}
