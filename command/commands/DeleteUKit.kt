package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object DeleteUKit : ClientCommand("deleteukit", description = "delete a kit") {

    init {
        string("name") { nameArg ->
            execute {
                val name = getValue(nameArg)
                if (Kit.kitMap.value.remove(name) != null) {
                    NoSpamMessage.sendMessage(KitCommand, "Deleted kit ${name.formatValue()}!")
                } else {
                    NoSpamMessage.sendWarning(KitCommand, "Kit ${name.formatValue()} not found!")
                }
            }
        }
    }
}
