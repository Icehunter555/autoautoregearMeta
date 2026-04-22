package dev.wizard.meta.event

import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient

open class SafeClientEvent(
    override val world: WorldClient,
    override val player: EntityPlayerSP,
    override val playerController: PlayerControllerMP,
    override val connection: NetHandlerPlayClient
) : AbstractClientEvent() {

    fun <T> invoke(block: SafeClientEvent.() -> T): T {
        return block()
    }

    companion object : ListenerOwner() {
        var instance: SafeClientEvent? = null
            private set

        init {
            listener<ConnectionEvent.Disconnect>(priority = Int.MAX_VALUE, alwaysListening = true) {
                reset()
            }
            listener<WorldEvent.Unload>(priority = Int.MAX_VALUE, alwaysListening = true) {
                reset()
            }
            listener<RunGameLoopEvent.Tick>(priority = Int.MAX_VALUE, alwaysListening = true) {
                update()
            }
        }

        fun update() {
            val world = Wrapper.world ?: return
            val player = Wrapper.player ?: return
            val playerController = Wrapper.minecraft.playerController ?: return
            val connection = Wrapper.minecraft.connection ?: return
            instance = SafeClientEvent(world, player, playerController, connection)
        }

        fun reset() {
            instance = null
        }
    }
}
