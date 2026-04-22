package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.render.XRay
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object XRayCommand : ClientCommand("xray", description = "Manage visible XRay blocks") {

    init {
        literal("add", "+") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (XRay.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(this@XRayCommand, "${blockName.formatValue()} is already added to the visible block list")
                    } else {
                        XRay.blockList.apply {
                            add(blockName)
                            editListeners.forEach { it(value) }
                        }
                        NoSpamMessage.sendMessage(this@XRayCommand, "${blockName.formatValue()} has been added to the visible block list")
                    }
                }
            }
        }

        literal("remove", "-") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    if (!XRay.blockList.contains(blockName)) {
                        NoSpamMessage.sendError(this@XRayCommand, "You do not have ${blockName.formatValue()} added to XRay visible block list")
                    } else {
                        XRay.blockList.apply {
                            remove(blockName)
                            editListeners.forEach { it(value) }
                        }
                        NoSpamMessage.sendMessage(this@XRayCommand, "Removed ${blockName.formatValue()} from XRay visible block list")
                    }
                }
            }
        }

        literal("set", "=") {
            block("block") { blockArg ->
                execute {
                    val blockName = getValue(blockArg).registryName.toString()
                    XRay.blockList.apply {
                        clear()
                        add(blockName)
                        editListeners.forEach { it(value) }
                    }
                    NoSpamMessage.sendMessage(this@XRayCommand, "Set the XRay block list to ${blockName.formatValue()}")
                }
            }
        }

        literal("reset", "default") {
            execute {
                XRay.blockList.apply {
                    resetValue()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendMessage(this@XRayCommand, "Reset the visible block list to defaults")
            }
        }

        literal("list") {
            execute {
                NoSpamMessage.sendMessage(this@XRayCommand, XRay.blockList.joinToString())
            }
        }

        literal("clear") {
            execute {
                XRay.blockList.apply {
                    clear()
                    editListeners.forEach { it(value) }
                }
                NoSpamMessage.sendMessage(this@XRayCommand, "Cleared the visible block list")
            }
        }
    }
}
