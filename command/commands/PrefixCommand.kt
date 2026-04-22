package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object PrefixCommand : ClientCommand("prefix", description = "Change command prefix") {

    init {
        literal("reset") {
            execute("Reset the prefix to ;") {
                Settings.prefix = ";"
                NoSpamMessage.sendMessage(this@PrefixCommand, "Reset prefix to [;.formatValue()]!")
            }
        }

        string("new prefix") { prefixArg ->
            execute("Set a new prefix") {
                val newPrefix = getValue(prefixArg)
                if (newPrefix.isEmpty() || newPrefix == "\") {
                    Settings.prefix = ";"
                    NoSpamMessage.sendMessage(this@PrefixCommand, "Reset prefix to [;.formatValue()]!")
                } else {
                    Settings.prefix = newPrefix
                    NoSpamMessage.sendMessage(this@PrefixCommand, "Set prefix to ${newPrefix.formatValue()}!")
                }
            }
        }
    }
}
