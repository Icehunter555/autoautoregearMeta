package dev.wizard.meta.command.commands

import dev.wizard.meta.MetaMod
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.modules.misc.AutoReconnect
import net.minecraft.network.play.client.CPacketPlayer

object KickMeCommand : ClientCommand("kickme", arrayOf("kick", "dc", "disconnect"), "kick u from server") {

    init {
        executeSafe("kick u") {
            MetaMod.logger.info("Player disconnected themselves")
            connection.sendPacket(CPacketPlayer.Position(Double.POSITIVE_INFINITY, 255.0, Double.POSITIVE_INFINITY, true))
            if (AutoReconnect.isEnabled) {
                AutoReconnect.hasKicked = true
            }
        }
    }
}
