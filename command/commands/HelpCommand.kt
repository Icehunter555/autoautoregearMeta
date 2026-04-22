package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.formatValue
import net.minecraft.util.text.TextFormatting

object HelpCommand : ClientCommand("help", description = "Help for commands") {

    init {
        string("command") { commandArg ->
            execute("List help for a command") {
                val command = CommandManager.getCommandOrNull(getValue(commandArg))
                if (command == null) {
                    MessageSendUtils.sendErrorMessage("Could not find command ${getValue(commandArg).formatValue()}!")
                } else {
                    MessageSendUtils.sendChatMessage("Help for command ${(prefix + command.name).formatValue()}\n${command.printArgHelp()}")
                }
            }
        }

        execute("List available commands") {
            val commands = CommandManager.getCommands().sortedBy { it.name.toString() }
            MessageSendUtils.sendChatMessage("Available commands: ${commands.size.formatValue()}")
            commands.forEach {
                MessageSendUtils.sendRawMessage("    $prefix${it.name}\n        ${TextFormatting.GRAY}${it.description}")
            }
        }
    }
}
