package dev.wizard.meta.command

import dev.wizard.meta.command.execute.IExecuteEvent
import dev.wizard.meta.util.collections.AliasSet
import java.util.*

abstract class AbstractCommandManager<E : IExecuteEvent> {
    private val commands = AliasSet<Command<E>>()
    private val builderCommandMap = HashMap<CommandBuilder<E>, Command<E>>()
    protected val lockObject = Any()

    fun register(builder: CommandBuilder<E>): Command<E> {
        return synchronized(lockObject) {
            val command = builder.buildCommand$Meta()
            commands.add(command)
            builderCommandMap[builder] = command
            command
        }
    }

    fun unregister(builder: CommandBuilder<E>): Command<E>? {
        return synchronized(lockObject) {
            builderCommandMap.remove(builder)?.also {
                commands.remove(it)
            }
        }
    }

    fun getCommands(): Set<Command<E>> {
        return commands.toSet()
    }

    fun getCommand(name: String): Command<E> {
        return commands.get(name) ?: throw CommandNotFoundException(name)
    }

    fun getCommandOrNull(name: String): Command<E>? {
        return commands.get(name)
    }

    open suspend fun invoke(event: E) {
        val name = event.args.getOrNull(0) ?: throw IllegalArgumentException("Arguments can not be empty!")
        getCommand(name).invoke(event)
    }

    fun parseArguments(string: String): Array<String> {
        if (string.isBlank()) {
            throw if (string.isEmpty()) IllegalArgumentException("Input can not be empty!") else IllegalArgumentException("Input can not be blank!")
        }
        return splitRegex.split(string.trim(), 0).map {
            it.removeSurrounding("\"").replace("''", "\"")
        }.toTypedArray()
    }

    companion object {
        private val splitRegex = Regex(" (?=(?:[^"]*\"[^"]*\")*[^"]*$)")
    }
}
