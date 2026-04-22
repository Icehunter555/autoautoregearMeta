package dev.wizard.meta.command.args

import dev.wizard.meta.util.interfaces.Nameable

abstract class AbstractArg<T : Any> : Nameable {
    protected open val typeName: String = this::class.java.simpleName.removeSuffix("Arg")
    protected val argTreeInternal = ArrayList<AbstractArg<*>>()

    val identifier: ArgIdentifier<T> by lazy { ArgIdentifier(name) }

    abstract override val name: String

    fun getArgTree(): List<AbstractArg<*>> = argTreeInternal.toList()

    open suspend fun checkType(string: String?): Boolean {
        return convertToType(string) != null
    }

    abstract suspend fun convertToType(string: String?): T?

    fun <T : Any> append(arg: AbstractArg<T>): AbstractArg<T> {
        if (this is FinalArg<*>) {
            throw IllegalArgumentException("${this::class.java.simpleName} can't be appended")
        }
        arg.argTreeInternal.addAll(this.argTreeInternal)
        arg.argTreeInternal.add(this)
        return arg
    }

    override fun toString(): String {
        return "<$name:$typeName>"
    }
}
