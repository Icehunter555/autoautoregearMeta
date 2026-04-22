package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.text.TextFormatting

object PlayerInfoCommand : ClientCommand("playerinfo", arrayOf("pi", "info-player"), "gets player info") {

    private fun SafeClientEvent.getPlayerInfo(profile: PlayerProfile) {
        val isFriend = FriendManager.isFriend(profile.name)
        val nameColor = if (isFriend) TextFormatting.GREEN else TextFormatting.LIGHT_PURPLE
        val separator = "${TextFormatting.GRAY}${TextFormatting.STRIKETHROUGH}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        MessageSendUtils.sendChatMessage(separator)
        MessageSendUtils.sendChatMessage("${TextFormatting.LIGHT_PURPLE}${TextFormatting.BOLD}Player Info ${TextFormatting.GRAY}│ $nameColor${profile.name}")
        MessageSendUtils.sendChatMessage(separator)
        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}UUID ${TextFormatting.DARK_GRAY}│ ${TextFormatting.DARK_GRAY}${profile.uuid}")
        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}Ping ${TextFormatting.DARK_GRAY}│ ${getPingWithColor(profile)}")

        if (playerIsLoaded(profile)) {
            val entity = getEntityFromProfile(profile)
            if (entity != null) {
                MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}Health ${TextFormatting.DARK_GRAY}│ ${getHealthWithColor(entity)}")
            }
        } else {
            MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}Health ${TextFormatting.DARK_GRAY}│ ${TextFormatting.YELLOW}Player not loaded")
        }

        val friendStatus = if (isFriend) "${TextFormatting.GREEN}✓ Friend" else "${TextFormatting.RED}✗ Not a friend"
        MessageSendUtils.sendChatMessage("${TextFormatting.GRAY}Status ${TextFormatting.DARK_GRAY}│ $friendStatus")
        MessageSendUtils.sendChatMessage(separator)
    }

    private fun getHealthWithColor(target: EntityPlayer): String {
        val health = MathUtils.round(target.health, 1)
        val maxHealth = MathUtils.round(target.maxHealth, 1)
        val color = when {
            health < 5.0f -> TextFormatting.DARK_RED
            health < 10.0f -> TextFormatting.RED
            health < 15.0f -> TextFormatting.GOLD
            health < maxHealth -> TextFormatting.YELLOW
            else -> TextFormatting.GREEN
        }
        return "$color$health${TextFormatting.GRAY}/${TextFormatting.WHITE}$maxHealth ${TextFormatting.RED}❤"
    }

    private fun SafeClientEvent.playerIsLoaded(profile: PlayerProfile): Boolean {
        return world.playerEntities.any { it.uniqueID == profile.uuid }
    }

    private fun SafeClientEvent.getEntityFromProfile(profile: PlayerProfile): EntityPlayer? {
        return world.playerEntities.firstOrNull { it.uniqueID == profile.uuid }
    }

    private fun SafeClientEvent.getPingWithColor(profile: PlayerProfile): String {
        val networkPlayerInfo = connection.getPlayerInfo(profile.uuid)
        val ping = networkPlayerInfo?.responseTime ?: 0
        val color = when {
            ping == 0 -> TextFormatting.GRAY
            ping < 50 -> TextFormatting.GREEN
            ping < 100 -> TextFormatting.YELLOW
            ping < 150 -> TextFormatting.GOLD
            else -> TextFormatting.RED
        }
        return if (ping == 0) "${color}Unknown${TextFormatting.RESET}" else "${color}${ping}ms${TextFormatting.RESET}"
    }

    init {
        player("player") { playerArg ->
            executeSafe("gets info about a player") {
                getPlayerInfo(getValue(playerArg))
            }
        }
    }
}
