package dev.wizard.meta.command

import dev.wizard.meta.command.args.*
import dev.wizard.meta.command.execute.ExecuteOption
import dev.wizard.meta.command.execute.IExecuteEvent

open class CommandBuilder<E : IExecuteEvent> @JvmOverloads constructor(
    name: String,
    alias: Array<String> = emptyArray(),
    private val description: String = "No description"
) : LiteralArg(name, alias) {

    protected val finalArgs = ArrayList<FinalArg<E>>()

    @CommandBuilder
    protected fun AbstractArg<*>.execute(
        description: String = "No description",
        options: Array<ExecuteOption<E>> = emptyArray(),
        block: ExecuteBlock<E>
    ) {
        val arg = FinalArg(description, options, block)
        append(arg)
        finalArgs.add(arg)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.execute(
        options: Array<ExecuteOption<E>>,
        block: ExecuteBlock<E>
    ) {
        execute("No description", options, block)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.boolean(name: String, block: BuilderBlock<Boolean>) {
        val arg = BooleanArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected inline fun <reified T : Enum<T>> AbstractArg<*>.enum(name: String, block: BuilderBlock<T>) {
        val arg = EnumArg(name, T::class.java)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.long(name: String, block: BuilderBlock<Long>) {
        val arg = LongArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.int(name: String, block: BuilderBlock<Int>) {
        val arg = IntArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.float(name: String, block: BuilderBlock<Float>) {
        val arg = FloatArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.double(name: String, block: BuilderBlock<Double>) {
        val arg = DoubleArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.literal(name: String, vararg alias: String, block: LiteralArg.() -> Unit) {
        val arg = LiteralArg(name, alias as Array<String>)
        append(arg)
        block(arg)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.string(name: String, block: BuilderBlock<String>) {
        val arg = StringArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.greedy(name: String, block: BuilderBlock<String>) {
        val arg = GreedyStringArg(name)
        append(arg)
        block(arg, arg.identifier)
    }

    @CommandBuilder
    protected fun <T> AbstractArg<*>.arg(arg: AbstractArg<T>, block: BuilderBlock<T>) {
        append(arg)
        block(arg, arg.identifier)
    }

    internal fun buildCommand$Meta(): Command<E> {
        return Command(name, alias, description, finalArgs.toTypedArray(), this)
    }

    @DslMarker
    @Retention(AnnotationRetention.RUNTIME)
    protected annotation class CommandBuilder
}
