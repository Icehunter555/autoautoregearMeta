package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import net.minecraft.network.play.client.CPacketPlayer

object VClipCommand : ClientCommand("vclip", description = "Attempts to clip vertically.") {

    init {
        double("offset") { offsetArg ->
            executeSafe {
                val posX = player.posX
                val posY = player.posY + getValue(offsetArg)
                val posZ = player.posZ
                val onGround = player.onGround

                player.setPosition(posX, posY, posZ)
                connection.sendPacket(CPacketPlayer.Position(posX, posY, posZ, onGround))
            }
        }
    }
}
