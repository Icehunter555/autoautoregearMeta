package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.util.inventory.slot.inventorySlots
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object KitCommand : ClientCommand("kit", description = "Kit management") {

    init {
        literal("create", "add", "new", "+") {
            string("name") { nameArg ->
                executeSafe {
                    val slots = player.inventorySlots
                    val array = List(36) { index ->
                        val stack = slots[index].stack
                        Kit.ItemEntry.fromStack(stack).toString()
                    }
                    val name = getValue(nameArg)
                    if (Kit.kitMap.value.put(name, array) == null) {
                        NoSpamMessage.sendMessage(this@KitCommand, "Created kit ${name.formatValue()}!")
                    } else {
                        NoSpamMessage.sendWarning(this@KitCommand, "Override kit ${name.formatValue()}!")
                    }
                }
            }
        }

        literal("delete", "del", "remove", "-") {
            string("name") { nameArg ->
                execute {
                    val name = getValue(nameArg)
                    if (Kit.kitMap.value.remove(name) != null) {
                        NoSpamMessage.sendMessage(this@KitCommand, "Deleted kit ${name.formatValue()}!")
                    } else {
                        NoSpamMessage.sendWarning(this@KitCommand, "Kit ${name.formatValue()} not found!")
                    }
                }
            }
        }

        literal("set", "=") {
            string("name") { nameArg ->
                execute {
                    val name = getValue(nameArg)
                    if (Kit.kitMap.value.containsKey(name)) {
                        Kit.kitName = name
                        NoSpamMessage.sendMessage(this@KitCommand, "Set kit to ${name.formatValue()}!")
                    } else {
                        NoSpamMessage.sendWarning(this@KitCommand, "Kit ${name.formatValue()} not found!")
                    }
                }
            }
        }

        literal("list") {
            execute {
                val kits = Kit.kitMap.value.keys.joinToString()
                NoSpamMessage.sendMessage(this@KitCommand, "List of kits:\n$kits")
            }
        }
    }
}
