package dev.wizard.meta.util

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.gui.GuiManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.client.FastLatency
import dev.wizard.meta.module.modules.misc.PingSpoof
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer

object InfoCalculator {
    private val mc = Wrapper.getMinecraft()

    fun getServerType(): String {
        return if (mc.isSingleplayer) {
            "Singleplayer"
        } else {
            mc.currentServerData?.serverIP ?: "Limbo"
        }
    }

    fun ping(): Int {
        val player = mc.player ?: return -1
        return mc.connection?.getPlayerInfo(player.uniqueID)?.responseTime ?: 1
    }

    fun getPlayerPing(profile: PlayerProfile): Int {
        return mc.connection?.getPlayerInfo(profile.uuid)?.responseTime ?: -1
    }

    fun getPlayerPing(entityPlayer: EntityPlayer): Int {
        val profile = entityPlayer.gameProfile
        return getPlayerPing(PlayerProfile(profile.id, profile.name))
    }

    fun speed(): Double {
        val tps = 1000.0 / TimerManager.tickLength
        return MovementUtils.getRealSpeed(mc.player) * tps
    }

    fun getFullPing(): Int {
        val normalPing = mc.connection?.getPlayerInfo(mc.player?.uniqueID ?: return -1)?.responseTime ?: -1
        val minus = if (PingSpoof.isEnabled) PingSpoof.delay else 0
        
        return when {
            FastLatency.isEnabled -> FastLatency.lastPacketPing.toInt()
            normalPing <= 1 -> normalPing
            normalPing - minus >= 15 -> normalPing - minus
            else -> normalPing
        }
    }

    fun getModifiedPing(): Int {
        val ping = mc.connection?.getPlayerInfo(mc.player?.uniqueID ?: return -1)?.responseTime ?: 1
        return ping - (if (PingSpoof.isEnabled) PingSpoof.delay else 0)
    }

    fun dimension(): String {
        return when (mc.player?.dimension) {
            -1 -> "Nether"
            0 -> "Overworld"
            1 -> "End"
            else -> "No Dimension"
        }
    }

    fun onlinePlayerCount(): Int {
        if (mc.isSingleplayer) return 1
        return mc.connection?.playerInfoMap?.size ?: 0
    }

    fun onlineFriendCount(): Int {
        if (mc.isSingleplayer) return 1
        val playerInfoMap = mc.connection?.playerInfoMap ?: return 0
        return playerInfoMap.count { FriendManager.isFriend(it.gameProfile.name) }
    }

    fun moduleCount(): Int {
        return ModuleManager.modules.count { !it.isDevOnly }
    }

    fun hudElementCount(): Int {
        return GuiManager.hudElements.size
    }

    fun commandCount(): Int {
        return CommandManager.commands.size
    }

    fun getFps(): Int {
        return Minecraft.getDebugFPS()
    }
}
