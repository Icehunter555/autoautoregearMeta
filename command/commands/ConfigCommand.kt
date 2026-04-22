package dev.wizard.meta.command.commands

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.command.execute.IExecuteEvent
import dev.wizard.meta.event.SafeExecuteEvent
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ConfigCommand : ClientCommand("config", arrayOf("cfg"), "Change config saving path or manually save and reload your config") {

    private val confirmTimer = TickTimer(TimeUnit.SECONDS)
    private var lastArgs = emptyArray<String>()

    private fun SafeExecuteEvent.getIpOrNull(): String? {
        val serverData = mc.currentServerData
        val ip = serverData?.serverIP
        return if (ip == null || mc.isSingleplayer) {
            NoSpamMessage.sendWarning("You are not in a server!")
            null
        } else {
            ip
        }
    }

    private fun IExecuteEvent.confirm(): Boolean {
        if (!args.contentEquals(lastArgs) || confirmTimer.tick(8L)) {
            NoSpamMessage.sendWarning("This can't be undone, run ${(prefix + args.joinToString(" ")).formatValue()} to confirm!")
            confirmTimer.reset()
            lastArgs = args
            return false
        }
        lastArgs = emptyArray()
        return true
    }

    init {
        literal("all") {
            literal("reload") {
                execute("Reload all configs") {
                    DefaultScope.launch(Dispatchers.IO) {
                        if (ConfigUtils.loadAll()) {
                            NoSpamMessage.sendMessage("All configurations reloaded!")
                        } else {
                            NoSpamMessage.sendError("Failed to load config!")
                        }
                    }
                }
            }

            literal("save") {
                execute("Save all configs") {
                    DefaultScope.launch(Dispatchers.IO) {
                        if (ConfigUtils.saveAll()) {
                            NoSpamMessage.sendMessage("All configurations saved!")
                        } else {
                            NoSpamMessage.sendError("Failed to load config!")
                        }
                    }
                }
            }
        }

        enum<Settings.ConfigType>("config type") { configTypeArg ->
            literal("reload") {
                execute("Reload a config") {
                    getValue(configTypeArg).reload()
                }
            }

            literal("save") {
                execute("Save a config") {
                    getValue(configTypeArg).save()
                }
            }

            literal("set") {
                string("name") { nameArg ->
                    execute("Change preset") {
                        getValue(configTypeArg).setPreset(getValue(nameArg))
                    }
                }
            }

            literal("copy", "ctrl+c", "ctrtc") {
                string("name") { nameArg ->
                    execute("Copy current preset to specific preset") {
                        val name = getValue(nameArg)
                        if (confirm()) {
                            getValue(configTypeArg).copyPreset(name)
                        }
                    }
                }
            }

            literal("delete", "del", "remove") {
                string("name") { nameArg ->
                    execute("Delete specific preset") {
                        val name = getValue(nameArg)
                        if (confirm()) {
                            getValue(configTypeArg).deletePreset(name)
                        }
                    }
                }
            }

            literal("list") {
                execute("List all available presets") {
                    getValue(configTypeArg).printAllPresets()
                }
            }

            literal("server") {
                literal("create", "new", "add") {
                    executeSafe("Create a new server preset") {
                        val ip = getIpOrNull() ?: return@executeSafe
                        getValue(configTypeArg).newServerPreset(ip)
                    }
                }

                literal("delete", "del", "remove") {
                    executeSafe("Delete the current server preset") {
                        val ip = getIpOrNull() ?: return@executeSafe
                        val configType = getValue(configTypeArg)
                        if (!configType.serverPresets.contains(ip)) {
                            NoSpamMessage.sendMessage("This server doesn't have a preset in config ${configType.displayName}")
                            return@executeSafe
                        }
                        if (confirm()) {
                            configType.deleteServerPreset(ip)
                        }
                    }
                }

                literal("list") {
                    execute("List all available server presets") {
                        getValue(configTypeArg).printAllServerPreset()
                    }
                }
            }

            execute("Print current preset name") {
                getValue(configTypeArg).printCurrentPreset()
            }
        }
    }
}
