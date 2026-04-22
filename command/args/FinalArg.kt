package dev.wizard.meta.command.args

import dev.wizard.meta.command.Invokable
import dev.wizard.meta.command.execute.ExecuteOption
import dev.wizard.meta.command.execute.IExecuteEvent

class FinalArg<E : IExecuteEvent>(
    private val description: String,
    private val options: Array<out ExecuteOption<E>>,
    private val block: suspend E.() -> Unit
) : AbstractArg<Unit>(), Invokable<E> {

    override val name: String get() = argTreeInternal.joinToString(".")

    override suspend fun convertToType(string: String?): Unit? {
        return if (string == null) Unit else null
    }

    suspend fun checkArgs(argsIn: Array<String>): Boolean {
        val lastArgType = argTreeInternal.lastOrNull() ?: return argsIn.isEmpty()
        if (!(argsIn.size == argTreeInternal.size ||
            (argsIn.size - 1 == argTreeInternal.size && argsIn.last().isBlank()) ||
            (argsIn.size > argTreeInternal.size && lastArgType is GreedyStringArg))) {
            return false
        }
        return countArgs(argsIn) == argTreeInternal.size
    }

    suspend fun countArgs(argsIn: Array<String>): Int {
        var matched = 0
        for ((index, argType) in argTreeInternal.withIndex()) {
            if (argType is GreedyStringArg) {
                matched++
            } else {
                if (argType.checkType(argsIn.getOrNull(index))) {
                    matched++
                } else {
                    break
                }
            }
        }
        return matched
    }

    override suspend fun invoke(event: E) {
        event.mapArgs(argTreeInternal)
        for (option in options) {
            if (!option.canExecute(event)) {
                option.onFailed(event)
                return
            }
        }
        block(event)
    }

    override fun toString(): String {
        return if (description.isNotBlank()) "- $description" else ""
    }

    fun printArgHelp(): String {
        val first = argTreeInternal.firstOrNull()?.name ?: return ""
        val others = argTreeInternal.subList(1, argTreeInternal.size)
        return "$first${others.joinToString(" ", prefix = " ")}".trimEnd()
    }
}
