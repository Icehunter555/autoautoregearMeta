package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.util.text.TextFormatting

object ToggleCommand : ClientCommand("toggle", arrayOf("switch", "t"), "Toggle a module on and off!") {

    init {
        module("module") { moduleArg ->
            execute {
                val module = getValue(moduleArg)
                if (module.isDevOnly || ModuleManager.getModuleOrNull(module.nameAsString) == null) {
                    NoSpamMessage.sendError("$chatName module ${TextFormatting.DARK_RED}${module.name}${TextFormatting.RESET} does not exist!")
                    return@execute
                }
                if (module.alwaysEnabled) {
                    NoSpamMessage.sendError("$chatName module ${TextFormatting.YELLOW}${module.name}${TextFormatting.RESET} cannot be toggled!")
                    return@execute
                }
                module.toggle()
                val status = if (module.isEnabled) "${TextFormatting.GREEN} on" else "${TextFormatting.RED} off"
                NoSpamMessage.sendMessage("$chatName toggled ${module.name} $status${TextFormatting.RESET}")
            }

            literal("off") {
                execute("Toggle a module off") {
                    val module = getValue(moduleArg)
                    if (module.isDevOnly || ModuleManager.getModuleOrNull(module.nameAsString) == null) {
                        NoSpamMessage.sendError("$chatName module ${TextFormatting.DARK_RED}${module.name}${TextFormatting.RESET} does not exist!")
                        return@execute
                    }
                    if (module.alwaysEnabled) {
                        NoSpamMessage.sendError("$chatName module ${TextFormatting.YELLOW}${module.name}${TextFormatting.RESET} cannot be toggled!")
                        return@execute
                    }
                    module.disable()
                    NoSpamMessage.sendMessage("$chatName toggled ${module.name} ${TextFormatting.RED}off${TextFormatting.RESET}")
                }
            }

            literal("on") {
                execute("Toggle a module on") {
                    val module = getValue(moduleArg)
                    if (module.isDevOnly || ModuleManager.getModuleOrNull(module.nameAsString) == null) {
                        NoSpamMessage.sendError("$chatName module ${TextFormatting.DARK_RED}${module.name}${TextFormatting.RESET} does not exist!")
                        return@execute
                    }
                    if (module.alwaysEnabled) {
                        NoSpamMessage.sendError("$chatName module ${TextFormatting.YELLOW}${module.name}${TextFormatting.RESET} cannot be toggled!")
                        return@execute
                    }
                    module.enable()
                    NoSpamMessage.sendMessage("$chatName toggled ${module.name} ${TextFormatting.GREEN}on${TextFormatting.RESET}")
                }
            }
        }

        literal("all") {
            literal("on") {
                execute("Toggle all modules on") {
                    var toggledCount = 0
                    ModuleManager.modules.forEach {
                        if (!it.isEnabled && !it.alwaysEnabled && !it.isDevOnly) {
                            it.enable()
                            toggledCount++
                        }
                    }
                    NoSpamMessage.sendMessage("$chatName toggled $toggledCount modules ${TextFormatting.GREEN}on${TextFormatting.RESET}")
                }
            }

            literal("off") {
                execute("Toggle all modules off") {
                    var toggledCount = 0
                    ModuleManager.modules.forEach {
                        if (it.isEnabled && !it.alwaysEnabled && !it.isDevOnly) {
                            it.disable()
                            toggledCount++
                        }
                    }
                    NoSpamMessage.sendMessage("$chatName toggled $toggledCount modules ${TextFormatting.RED}off${TextFormatting.RESET}")
                }
            }
        }
    }
}
