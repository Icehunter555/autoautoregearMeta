package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.util.KeyboardUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import net.minecraft.util.text.TextFormatting

object BindCommand : ClientCommand("bind", description = "Bind and unbind modules") {

    init {
        literal("list") {
            execute("List used module binds") {
                val binds = ModuleManager.modules.filter { !it.bind.value.isEmpty }
                val bindsText = binds.joinToString("\n") {
                    "${TextFormatting.BOLD}${it.name}${TextFormatting.RESET} : ${TextFormatting.LIGHT_PURPLE}${it.bind}${TextFormatting.RESET}"
                }
                NoSpamMessage.sendMessage(if (bindsText.isEmpty()) "No binds set." else "${TextFormatting.BOLD}Binds:${TextFormatting.RESET}\n$bindsText")
            }
        }

        literal("reset", "unbind") {
            module("module") { moduleArg ->
                execute("Reset the bind of a module to nothing") {
                    val module = getValue(moduleArg)
                    module.bind.resetValue()
                    NoSpamMessage.sendMessage("Reset bind for ${module.name}!")
                }
            }

            literal("all") {
                execute("Reset all binds") {
                    ModuleManager.modules.forEach {
                        if (!it.bind.value.isEmpty) {
                            it.bind.resetValue()
                        }
                    }
                    NoSpamMessage.sendMessage("Reset all binds!")
                }
            }
        }

        module("module") { moduleArg ->
            string("bind") { bindArg ->
                execute("Bind a module to a key") {
                    val module = getValue(moduleArg)
                    val bind = getValue(bindArg)

                    if (bind.equals("None", ignoreCase = true)) {
                        module.bind.resetValue()
                        NoSpamMessage.sendMessage("Reset bind for ${module.name}!")
                    } else {
                        val key = KeyboardUtils.getKey(bind)
                        if (key in 1..255) {
                            module.bind.setValue(bind)
                            NoSpamMessage.sendMessage("Bind for ${module.name} set to ${module.bind.formatValue()}!")
                        } else {
                            KeyboardUtils.sendUnknownKeyError(bind)
                        }
                    }
                }
            }

            execute("Get the bind of a module") {
                val module = getValue(moduleArg)
                NoSpamMessage.sendMessage("${module.name} is bound to ${module.bind.formatValue()}")
            }
        }
    }
}
