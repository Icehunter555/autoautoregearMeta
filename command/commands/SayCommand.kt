package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.command.CommandManager

object SayCommand : ClientCommand("say", description = "sends a message in chat") {

    init {
        greedy("The message to send") { msgArg ->
            executeSafe {
                val msg = getValue(msgArg)
                if (msg.startsWith("/") || msg.startsWith(CommandManager.prefix) || msg.startsWith("#")) {
                    msg.drop(0)
                }
                player.sendChatMessage(msg)
            }
        }
    }
}
