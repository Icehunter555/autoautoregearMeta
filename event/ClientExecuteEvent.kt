package dev.wizard.meta.event

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.command.execute.ExecuteEvent
import dev.wizard.meta.command.execute.IExecuteEvent

class ClientExecuteEvent(args: Array<String>) : ClientEvent(), IExecuteEvent by ExecuteEvent(CommandManager, args)
