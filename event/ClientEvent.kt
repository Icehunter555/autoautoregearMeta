package dev.wizard.meta.event

import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient

open class ClientEvent : AbstractClientEvent() {
    override val world: WorldClient? = mc.world
    override val player: EntityPlayerSP? = mc.player
    override val playerController: PlayerControllerMP? = mc.playerController
    override val connection: NetHandlerPlayClient? = mc.connection

    fun <T> invoke(block: ClientEvent.() -> T): T {
        return block()
    }
}
