package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.modules.client.FastLatency
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.util.text.TextFormatting

object PingCommand : ClientCommand("ping", description = "shows ping") {

    private fun formatPublicPing(ping: Int): String {
        return if (ping == 0) "not calculated by the server yet" else "${ping}ms"
    }

    fun getPingWithColor(ping: Int): String {
        val color = when {
            ping == 0 -> TextFormatting.GRAY
            ping < 50 -> TextFormatting.GREEN
            ping < 100 -> TextFormatting.YELLOW
            ping < 150 -> TextFormatting.RED
            else -> TextFormatting.DARK_RED
        }
        return if (ping == 0) "${color}UnKnown${TextFormatting.RESET}" else "${color}${ping}${TextFormatting.BOLD}ms${TextFormatting.RESET}"
    }

    fun SafeClientEvent.getPing(profile: PlayerProfile): Int {
        return connection.getPlayerInfo(profile.uuid)?.responseTime ?: 0
    }

    init {
        executeSafe {
            val profile = PlayerProfile(player.gameProfile.id, player.gameProfile.name)
            val ping = getPing(profile)
            NoSpamMessage.sendMessage("Your ping is ${getPingWithColor(ping)}!")
        }

        literal("send") {
            executeSafe {
                val profile = PlayerProfile(player.gameProfile.id, player.gameProfile.name)
                val ping = getPing(profile)
                player.sendChatMessage("[PingCommand] My ping is ${formatPublicPing(ping)}!")
            }
        }

        literal("fast") {
            executeSafe {
                val ping = FastLatency.lastPacketPing.toInt()
                NoSpamMessage.sendMessage("Last fastlatency ping is ${getPingWithColor(ping)}")
            }

            literal("send") {
                executeSafe {
                    val ping = FastLatency.lastPacketPing.toInt()
                    player.sendChatMessage("[PingCommand] My fastlatency ping is ${formatPublicPing(ping)}")
                }
            }
        }

        player("player") { playerArg ->
            executeSafe {
                val profile = getValue(playerArg)
                val ping = getPing(profile)
                NoSpamMessage.sendMessage("${profile.name}'s ping is ${getPingWithColor(ping)}")
            }

            literal("send") {
                executeSafe {
                    val profile = getValue(playerArg)
                    val ping = getPing(profile)
                    player.sendChatMessage("[PingCommand] ${profile.name}'s ping is ${formatPublicPing(ping)}")
                }
            }
        }
    }
}
