package dev.wizard.meta.command

import dev.wizard.meta.command.args.FinalArg
import dev.wizard.meta.command.execute.IExecuteEvent
import dev.wizard.meta.util.interfaces.Alias
import dev.wizard.meta.util.interfaces.Nameable

class Command<E : IExecuteEvent>(
    override val name: CharSequence,
    override val alias: Array<out CharSequence>,
    val description: String,
    val finalArgs: Array<out FinalArg<E>>,
    val builder: CommandBuilder<E>
) : Nameable, Alias, Invokable<E> {

    override suspend fun invoke(event: E) {
        val finalArg = finalArgs.firstOrNull { it.checkArgs(event.args) }
        if (finalArg != null) {
            finalArg.invoke(event)
        } else {
            throw SubCommandNotFoundException(event.args, this)
        }
    }

    fun printArgHelp(): String {
        return finalArgs.joinToString("\n\n") {
            var argHelp = it.printArgHelp()
            val description = it.toString()
            if (argHelp.isBlank()) {
                argHelp = "<No Argument>"
            }
            if (description.isNotBlank()) {
                argHelp = "$argHelp\n$it"
            }
            argHelp
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Command<*>) return false
        if (name != other.name) return false
        if (!alias.contentEquals(other.alias)) return false
        if (description != other.description) return false
        if (!finalArgs.contentEquals(other.finalArgs)) return false
        if (builder != other.builder) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + alias.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + finalArgs.contentHashCode()
        result = 31 * result + builder.hashCode()
        return result
    }
}
