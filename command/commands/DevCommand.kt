package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.util.ClipboardUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.manager.managers.MetaManager
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.common.ForgeVersion
import java.util.*

object DevCommand : ClientCommand("dev", description = "dev command") {

    private fun getGoodUUID(uuid: String): String {
        return if (!MetaManager.isDevorfalse()) uuid else {
            uuid.replace("1", "$")
                .replace("7", "*")
                .replace("-", "")
        }
    }

    init {
        literal("troubleshoot") {
            execute("Print troubleshooting information") {
                NoSpamMessage.sendError("Send a screenshot of all information below this line!", false)
                val enabledModules = ModuleManager.modules.filter { it.isEnabled }.joinToString { it.name }
                NoSpamMessage.sendMessage("Enabled Modules:\n$enabledModules", false)
                NoSpamMessage.sendMessage("Meta 0.3B-10mq29", false)
                NoSpamMessage.sendMessage("Forge ${ForgeVersion.getMajorVersion()}.${ForgeVersion.getMinorVersion()}.${ForgeVersion.getRevisionVersion()}.${ForgeVersion.getBuildVersion()}", false)
                val osName = System.getProperty("os.name").lowercase(Locale.ROOT).capitalize()
                NoSpamMessage.sendMessage("Operating System: $osName ${System.getProperty("os.version")} ", false)
                NoSpamMessage.sendMessage("JVM: ${System.getProperty("java.version")} ${System.getProperty("java.vendor")}", false)
                NoSpamMessage.sendMessage("GPU: ${GlStateManager.glGetString(7936)}", false)
                NoSpamMessage.sendMessage("CPU: ${System.getProperty("os.arch")} ${OpenGlHelper.getCpu()}", false)
                NoSpamMessage.sendError("Please send a screenshot of the full output to Wizard_11!", false)
            }
        }

        literal("resetgamma") {
            executeSafe("Reset gamma") {
                mc.gameSettings.gammaSetting = 1.0f
            }
        }

        literal("unpress") {
            executeSafe("Unpresses all keys") {
                KeyBinding.unPressAllKeys()
            }
        }

        literal("disableHidden") {
            execute("disable all hidden modules") {
                var disabledCount = 0
                ModuleManager.modules.forEach {
                    if (it.isDevOnly && it.isEnabled && !it.alwaysEnabled) {
                        it.disable()
                        disabledCount++
                    }
                }
                NoSpamMessage.sendMessage("$chatName disabled $disabledCount modules!")
            }
        }

        literal("listhidden") {
            execute("list all hidden modules") {
                ModuleManager.modules.forEach {
                    if (it.isDevOnly) {
                        NoSpamMessage.sendMessage("${it.name} - hidden \n")
                    }
                }
            }
        }

        literal("getuuid", "copyuuid") {
            player("player") { playerArg ->
                executeSafe("copy a player's uuid") {
                    val uuid = getValue(playerArg).uuid.toString()
                    try {
                        ClipboardUtils.copyToClipboard(getGoodUUID(uuid))
                        NoSpamMessage.sendMessage("$chatName copied ${TextFormatting.DARK_GRAY}$uuid${TextFormatting.RESET} to clipboard!", false)
                    } catch (e: Exception) {
                        NoSpamMessage.sendError("Failed to copy $uuid to clipboard: ${e.message}")
                    }
                }
            }
        }

        literal("testchat") {
            executeSafe {
                NoSpamMessage.sendRaw("\u00a7*META \u00a7r did it work?", false)
            }
        }
    }
}
