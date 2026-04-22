package dev.wizard.meta.command.execute

interface ExecuteOption<E : IExecuteEvent> {
    suspend fun canExecute(event: E): Boolean
    suspend fun onFailed(event: E)
}
