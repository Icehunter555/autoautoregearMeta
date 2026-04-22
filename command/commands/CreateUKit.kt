package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.util.inventory.slot.inventorySlots
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object CreateUKit : ClientCommand("createukit", description = "create a kit") {

    init {
        string("name") { nameArg ->
            executeSafe {
                val slots = player.inventorySlots
                val array = List(36) { index ->
                    val stack = slots[index].stack
                    Kit.ItemEntry.fromStack(stack).toString()
                }
                val name = getValue(nameArg)
                if (Kit.kitMap.value.put(name, array) == null) {
                    NoSpamMessage.sendMessage(KitCommand, "Created kit ${name.formatValue()}!")
                } else {
                    NoSpamMessage.sendWarning(KitCommand, "Override kit ${name.formatValue()}!")
                }
            }
        }
    }
}
