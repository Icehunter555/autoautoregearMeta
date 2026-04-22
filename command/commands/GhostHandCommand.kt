package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.player.GhostHand
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object GhostHandCommand : ClientCommand("ghosthand", description = "Manage GhostHand block list") {

    init {
        literal("add", "+") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (GhostHand.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(this@GhostHandCommand, "${blockName.formatValue()} is already added to the visible block list")
                    } else {
                        GhostHand.blockList.apply {
                            add(blockName)
                            editListeners.forEach { it(value) }
                        }
                        NoSpamMessage.sendMessage(this@GhostHandCommand, "${blockName.formatValue()} has been added to the visible block list")
                    }
                }
            }
        }

        literal("remove", "-") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (!GhostHand.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(this@GhostHandCommand, "You do not have ${blockName.formatValue()} added to xray visible block list")
                    } else {
                        GhostHand.blockList.apply {
                            remove(blockName)
                            editListeners.forEach { it(value) }
                        }
                        NoSpamMessage.sendMessage(this@GhostHandCommand, "Removed ${blockName.formatValue()} from xray visible block list")
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    GhostHand.blockList.apply {
                        clear()
                        add(blockName)
                        editListeners.forEach { it(value) }
                    }
                    NoSpamMessage.sendMessage(this@GhostHandCommand, "Set the xray block list to ${blockName.formatValue()}")
                }
            }
        }

        literal("reset", "default") {
            execute {
                GhostHand.blockList.apply {
                    resetValue()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendMessage(this@GhostHandCommand, "Reset the visible block list to defaults")
            }
        }

        literal("list") {
            execute {
                NoSpamMessage.sendMessage(this@GhostHandCommand, GhostHand.blockList.joinToString())
            }
        }

        literal("clear") {
            execute {
                GhostHand.blockList.apply {
                    clear()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendMessage(this@GhostHandCommand, "Cleared the visible block list")
            }
        }
    }
}
