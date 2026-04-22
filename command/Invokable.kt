package dev.wizard.meta.command

import dev.wizard.meta.command.execute.IExecuteEvent

interface Invokable<E : IExecuteEvent> {
    suspend fun invoke(event: E)
}
