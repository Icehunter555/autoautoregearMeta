package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.player.Nuker
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object NukerCommand : ClientCommand("nuker", description = "configure the nuker module") {

    init {
        literal("add", "+") {
            block("block") { blockArg ->
                executeSafe {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (Nuker.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(this@NukerCommand, "${blockName.formatValue()} is already added to the blocks list!")
                    } else {
                        Nuker.blockList.apply {
                            add(blockName)
                            editListeners.forEach { it(value) }
                        }
                        NoSpamMessage.sendMessage(this@NukerCommand, "${blockName.formatValue()} has been added to the blocks list!")
                    }
                }
            }
        }

        literal("remove", "del", "-") {
            block("block") { blockArg ->
                executeSafe {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (!Nuker.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(this@NukerCommand, "${blockName.formatValue()} is not in the blocks list!")
                    } else {
                        Nuker.blockList.apply {
                            remove(blockName)
                            editListeners.forEach { it(value) }
                        }
                        NoSpamMessage.sendMessage(this@NukerCommand, "${blockName.formatValue()} has been removed from the blocks list!")
                    }
                }
            }
        }

        literal("list") {
            executeSafe {
                NoSpamMessage.sendMessage(this@NukerCommand, Nuker.blockList.joinToString())
            }
        }

        literal("clear") {
            executeSafe {
                Nuker.blockList.apply {
                    clear()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendWarning(this@NukerCommand, "cleared the block list!")
            }
        }

        literal("reset") {
            executeSafe {
                Nuker.blockList.apply {
                    resetValue()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendWarning(this@NukerCommand, "reset the block list!")
            }
        }
    }
}
