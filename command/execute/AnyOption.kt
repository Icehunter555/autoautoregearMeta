package dev.wizard.meta.command.execute

class AnyOption<E : IExecuteEvent>(private vararg val options: ExecuteOption<E>) : ExecuteOption<E> {

    override suspend fun canExecute(event: E): Boolean {
        return options.any { it.canExecute(event) }
    }

    override suspend fun onFailed(event: E) {
        options.last().onFailed(event)
    }
}
