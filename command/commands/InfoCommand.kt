package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.gui.GuiManager
import dev.wizard.meta.gui.hudgui.elements.text.TPS
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.misc.PingSpoof
import dev.wizard.meta.util.InfoCalculator
import dev.wizard.meta.util.threads.onMainThread
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.common.ForgeVersion
import java.util.*

object InfoCommand : ClientCommand("info", description = "displays info") {

    private fun sendClientInfo() {
        val safeUser = mc.session.username ?: "Random"
        send("${TextFormatting.BOLD}Meta : ${TextFormatting.RESET}${TextFormatting.LIGHT_PURPLE}0.3B-10mq29${TextFormatting.RESET}")

        val totalModules = ModuleManager.modules.size
        val devOnlyModules = ModuleManager.modules.count { it.isDevOnly }
        send("${TextFormatting.BOLD}Modules: ${TextFormatting.RESET}${TextFormatting.LIGHT_PURPLE} ${totalModules - devOnlyModules}${TextFormatting.RESET}")

        send("${TextFormatting.BOLD}Commands: ${TextFormatting.RESET}${TextFormatting.GOLD} ${CommandManager.getCommands().size}${TextFormatting.RESET}")
        send("${TextFormatting.BOLD}Hud Elements: ${TextFormatting.RESET}${TextFormatting.AQUA} ${GuiManager.hudElements.size}${TextFormatting.RESET}")
        send("")
        send("${TextFormatting.GRAY}Made by ${if (safeUser == "Wizard_11") "${TextFormatting.LIGHT_PURPLE}You${TextFormatting.RESET}" else "${TextFormatting.LIGHT_PURPLE}Wizard_11${TextFormatting.RESET}"}!")
        send("${TextFormatting.DARK_GRAY}forge ${ForgeVersion.getMajorVersion()}.${ForgeVersion.getMinorVersion()}.${ForgeVersion.getRevisionVersion()}.${ForgeVersion.getBuildVersion()} [${mc.version}]${TextFormatting.RESET}")
    }

    private fun sendMinecraftInfo() {
        send("${TextFormatting.BOLD}Minecraft: ${TextFormatting.RESET}Minecraft ${TextFormatting.GRAY}${mc.version}${TextFormatting.RESET}")
        send("")
        send("${TextFormatting.BOLD}Forge: ${TextFormatting.RESET}Forge ${ForgeVersion.getMajorVersion()}.${ForgeVersion.getMinorVersion()} ${TextFormatting.DARK_GRAY}(${ForgeVersion.getRevisionVersion()}.${ForgeVersion.getBuildVersion()})${TextFormatting.RESET}")

        val name = mc.player?.gameProfile?.name ?: "${TextFormatting.DARK_GRAY}Random${TextFormatting.RESET}"
        send("${TextFormatting.BOLD}User: ${TextFormatting.RESET}$name")

        val uuid = mc.player?.gameProfile?.id?.toString() ?: "${TextFormatting.DARK_GRAY}Unknown${TextFormatting.RESET}"
        send("${TextFormatting.BOLD}UUID: ${TextFormatting.RESET}${TextFormatting.GRAY}$uuid${TextFormatting.RESET}")
    }

    private fun sendServerInfo() {
        val server = getServerInfo()
        send("${TextFormatting.BOLD}Connected to ${TextFormatting.LIGHT_PURPLE}$server${TextFormatting.RESET}")
        send("")
        send("${TextFormatting.BOLD}TPS: ${TextFormatting.RESET}${getTpsWithColor()}/20.00${TextFormatting.RESET}")
        send("${TextFormatting.BOLD}Ping: ${TextFormatting.RESET}${getPingWithColor()}")
        send("${TextFormatting.BOLD}Online: ${TextFormatting.RESET}${TextFormatting.GRAY}${getOnlinePlayers()} ${TextFormatting.RESET}(${TextFormatting.GREEN}${getOnlineFriends()}${TextFormatting.RESET})${TextFormatting.RESET}")
        send("${TextFormatting.BOLD}Server Brand: ${TextFormatting.RESET}${getServerBrand()}${TextFormatting.RESET}")
    }

    private fun getServerInfo(): String {
        return when {
            mc.isSingleplayer -> "SinglePlayer"
            mc.currentServerData != null -> {
                val ip = mc.currentServerData?.serverIP ?: ""
                if (ip.isEmpty()) "Limbo" else ip
            }
            else -> "Limbo"
        }
    }

    fun getPingWithColor(): String {
        val ping = InfoCalculator.ping()
        val color = when {
            ping == 0 -> TextFormatting.GRAY
            ping < 50 -> TextFormatting.GREEN
            ping < 100 -> TextFormatting.YELLOW
            ping < 150 -> TextFormatting.RED
            else -> TextFormatting.DARK_RED
        }
        return if (ping == 0) "${color}UnKnown${TextFormatting.RESET}" else "${color}${ping}${TextFormatting.BOLD}ms${if (PingSpoof.isEnabled) " [+${PingSpoof.delay}]" else ""}${TextFormatting.RESET}"
    }

    fun getOnlineFriends(): Int {
        if (mc.isSingleplayer || mc.currentServerData == null) return 0
        val connection = mc.connection ?: return 0
        return connection.playerInfoMap
            .map { it.gameProfile.name }
            .count { FriendManager.isFriend(it) }
    }

    private fun getOnlinePlayers(): Int {
        if (mc.isSingleplayer) return 1
        return mc.connection?.playerInfoMap?.size ?: 1
    }

    fun getTpsWithColor(): String {
        val tps = TPS.tpsBuffer.average()
        val color = when {
            tps >= 18.0 -> TextFormatting.GREEN
            tps >= 15.0 -> TextFormatting.YELLOW
            tps >= 10.0 -> TextFormatting.GOLD
            tps >= 5.0 -> TextFormatting.RED
            else -> TextFormatting.DARK_RED
        }
        return "${color}${String.format("%.2f", tps)}${TextFormatting.RESET}"
    }

    fun getServerBrand(): String {
        val brand = mc.player?.serverBrand ?: "Unknown"
        return if (mc.isSingleplayer) {
            "${TextFormatting.DARK_GRAY}SinglePlayer ($brand) "
        } else {
            "${TextFormatting.GRAY}($brand) "
        }
    }

    private fun send(message: String) {
        onMainThread {
            mc.ingameGUI?.chatGUI?.printChatMessageWithOptionalId(TextComponentString("  $message"), message.hashCode())
        }
    }

    init {
        executeSafe("show info about the client") {
            sendClientInfo()
        }

        literal("client") {
            executeSafe("show info about the client") {
                sendClientInfo()
            }

            literal("clear") {
                executeSafe("clear chat and show info about the client") {
                    mc.ingameGUI.chatGUI.clearChatMessages(false)
                    sendClientInfo()
                }
            }
        }

        literal("server", "connection") {
            executeSafe("show info about your connection") {
                sendServerInfo()
            }

            literal("clear") {
                executeSafe("clear chat and show info about your connection") {
                    mc.ingameGUI.chatGUI.clearChatMessages(false)
                    sendServerInfo()
                }
            }
        }

        literal("minecraft", "mc") {
            executeSafe("show info about minecraft") {
                sendMinecraftInfo()
            }

            literal("clear") {
                executeSafe("clear chat and show info about minecraft") {
                    mc.ingameGUI.chatGUI.clearChatMessages(false)
                    sendMinecraftInfo()
                }
            }
        }
    }
}
