package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.render.Search
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object SearchCommand : ClientCommand("search", description = "Manage search blocks") {

    private val warningBlocks = hashSetOf("minecraft:grass", "minecraft:end_stone", "minecraft:lava", "minecraft:bedrock", "minecraft:netherrack", "minecraft:dirt", "minecraft:water", "minecraft:stone")

    private fun addBlock(blockName: String) {
        if (blockName == "minecraft:air") {
            NoSpamMessage.sendMessage("You can't add ${blockName.formatValue()} to the search block list")
            return
        }
        Search.searchList.apply {
            if (!add(blockName)) {
                NoSpamMessage.sendError("${blockName.formatValue()} is already added to the search block list")
            } else {
                NoSpamMessage.sendMessage("${blockName.formatValue()} has been added to the search block list")
            }
            editListeners.forEach { it(value) }
        }
    }

    init {
        literal("add", "+") {
            block("block") { blockArg ->
                literal("force") {
                    execute {
                        val blockName = getValue(blockArg).registryName.toString()
                        addBlock(blockName)
                    }
                }

                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (warningBlocks.contains(blockName)) {
                        NoSpamMessage.sendWarning("Your world contains lots of ${blockName.formatValue()}, it might cause extreme lag to add it. If you are sure you want to add it run ${("$prefixName add force $blockName").formatValue()}")
                    } else {
                        addBlock(blockName)
                    }
                }
            }
        }

        literal("remove", "del", "delete", "-") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    Search.searchList.apply {
                        if (!remove(blockName)) {
                            NoSpamMessage.sendError("You do not have ${blockName.formatValue()} added to search block list")
                        } else {
                            NoSpamMessage.sendMessage("Removed ${blockName.formatValue()} from search block list")
                        }
                        editListeners.forEach { it(value) }
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    Search.searchList.apply {
                        clear()
                        add(blockName)
                        editListeners.forEach { it(value) }
                    }
                    NoSpamMessage.sendMessage("Set the search block list to ${blockName.formatValue()}")
                }
            }
        }

        literal("reset", "default") {
            execute {
                Search.searchList.resetValue()
                NoSpamMessage.sendMessage("Reset the search block list to defaults")
            }
        }

        literal("list") {
            execute {
                NoSpamMessage.sendMessage(Search.searchList.joinToString())
            }
        }

        literal("clear") {
            execute {
                Search.searchList.apply {
                    clear()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendMessage("Cleared the search block list")
            }
        }
    }
}
