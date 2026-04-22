package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.util.text.TextFormatting

object OnlineCommand : ClientCommand("online", description = "gives u a list of online players") {

    init {
        executeSafe {
            val onlineProfiles = connection.playerInfoMap.map { it.gameProfile }
            if (onlineProfiles.isEmpty()) {
                MessageSendUtils.sendErrorMessage("${TextFormatting.GRAY}[${TextFormatting.AQUA}Online${TextFormatting.GRAY}] ${TextFormatting.RED}No players found")
                return@executeSafe
            }

            val playersInRange = world.playerEntities
                .filter { it != player }
                .map { it.name }
                .toSet()

            val formattedPlayers = onlineProfiles.map { profile ->
                val name = profile.name ?: "Unknown"
                when {
                    FriendManager.isFriend(name) -> "${TextFormatting.GREEN}$name"
                    playersInRange.contains(name) -> "${TextFormatting.RED}$name"
                    else -> "${TextFormatting.GRAY}$name"
                }
            }

            val separator = "${TextFormatting.GRAY}${TextFormatting.STRIKETHROUGH}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            MessageSendUtils.sendChatMessage(separator)
            MessageSendUtils.sendChatMessage("${TextFormatting.AQUA}${TextFormatting.BOLD}Online Players ${TextFormatting.GRAY}(${onlineProfiles.size} total)")
            MessageSendUtils.sendChatMessage(separator)

            formattedPlayers.chunked(5).forEach { chunk ->
                MessageSendUtils.sendChatMessage("${TextFormatting.DARK_GRAY}│ ${chunk.joinToString("${TextFormatting.DARK_GRAY}, ")}")
            }
            MessageSendUtils.sendChatMessage(separator)
        }
    }
}
