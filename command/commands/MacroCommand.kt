package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.manager.managers.MacroManager
import dev.wizard.meta.util.KeyboardUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue

object MacroCommand : ClientCommand("macro", arrayOf("m"), "Manage your command / message macros") {

    init {
        literal("list") {
            string("key") { keyArg ->
                execute("List macros for a key") {
                    val input = getValue(keyArg)
                    val key = KeyboardUtils.getKey(input)
                    if (key !in 1..255) {
                        KeyboardUtils.sendUnknownKeyError(input)
                        return@execute
                    }
                    val macros = MacroManager.macros.getOrNull(key) ?: emptyList()
                    val formattedName = (KeyboardUtils.getDisplayName(key) ?: "Unknown").formatValue()

                    if (macros.isEmpty()) {
                        NoSpamMessage.sendMessage("\u00a7cYou have no macros for the key $formattedName")
                    } else {
                        val stringBuilder = StringBuilder("You have has the following macros for $formattedName:\n")
                        macros.forEach {
                            stringBuilder.append("$formattedName $it\n")
                        }
                        NoSpamMessage.sendMessage(stringBuilder.toString())
                    }
                }
            }

            execute("List all macros") {
                if (MacroManager.isEmpty) {
                    NoSpamMessage.sendMessage("\u00a7cYou have no macros")
                } else {
                    val stringBuilder = StringBuilder("You have the following macros:\n")
                    MacroManager.macros.forEachIndexed { key, value ->
                        if (value.isNotEmpty()) {
                            val formattedName = (KeyboardUtils.getDisplayName(key) ?: "Unknown").formatValue()
                            stringBuilder.append("$formattedName $value\n")
                        }
                    }
                    NoSpamMessage.sendMessage(stringBuilder.toString())
                }
            }
        }

        literal("clear") {
            string("key") { keyArg ->
                execute("Clear macros for a key") {
                    val input = getValue(keyArg)
                    val key = KeyboardUtils.getKey(input)
                    if (key !in 1..255) {
                        KeyboardUtils.sendUnknownKeyError(input)
                        return@execute
                    }
                    val formattedName = (KeyboardUtils.getDisplayName(key) ?: "Unknown").formatValue()
                    MacroManager.removeMacro(key)
                    MacroManager.saveMacros()
                    MacroManager.loadMacros()
                    NoSpamMessage.sendMessage("Cleared macros for $formattedName")
                }
            }
        }

        literal("add") {
            string("key") { keyArg ->
                greedy("command / message") { macroArg ->
                    execute("Add a command / message for a key") {
                        val input = getValue(keyArg)
                        val key = KeyboardUtils.getKey(input)
                        if (key !in 1..255) {
                            KeyboardUtils.sendUnknownKeyError(input)
                            return@execute
                        }
                        val macro = getValue(macroArg)
                        val formattedName = (KeyboardUtils.getDisplayName(key) ?: "Unknown").formatValue()
                        MacroManager.addMacro(key, macro)
                        MacroManager.saveMacros()
                        NoSpamMessage.sendMessage("Added macro ${macro.formatValue()} for key $formattedName")
                    }
                }
            }
        }

        literal("set") {
            string("key") { keyArg ->
                greedy("command / message") { macroArg ->
                    execute("Set a command / message for a key") {
                        val input = getValue(keyArg)
                        val key = KeyboardUtils.getKey(input)
                        if (key !in 1..255) {
                            KeyboardUtils.sendUnknownKeyError(input)
                            return@execute
                        }
                        val macro = getValue(macroArg)
                        val formattedName = (KeyboardUtils.getDisplayName(key) ?: "Unknown").formatValue()
                        MacroManager.setMacro(key, macro)
                        MacroManager.saveMacros()
                        NoSpamMessage.sendMessage("Added macro ${macro.formatValue()} for key $formattedName")
                    }
                }
            }
        }
    }
}
