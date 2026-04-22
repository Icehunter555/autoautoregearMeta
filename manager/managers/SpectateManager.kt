package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.Manager
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketAnimation
import net.minecraft.util.EnumHand

object SpectateManager : Manager() {
    var spectateTarget: EntityPlayer? = null
    var spectating: Boolean = false

    fun disableSpectating() {
        spectating = false
        mc.renderViewEntity = mc.player
        spectateTarget = null
    }

    fun spectatePlayer(specPlayer: EntityPlayer) {
        spectateTarget = specPlayer
        mc.renderViewEntity = specPlayer
        spectating = true
    }

    fun isSpectating(): Boolean {
        return spectating && spectateTarget != null
    }

    init {
        safeListener<TickEvent.Pre> {
            if (!spectating) return@safeListener
            val target = spectateTarget
            if (target == null) {
                disableSpectating()
                return@safeListener
            }
            if (world.getEntityByID(target.entityId) == null) {
                disableSpectating()
            }
            if (mc.isSingleplayer) {
                disableSpectating()
            }
        }

        safeListener<PacketEvent.Send> { event ->
            if (!spectating || spectateTarget == null) return@safeListener
            val packet = event.packet
            if (packet is CPacketPlayer || packet is CPacketAnimation) {
                event.cancel()
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            if (!spectating || spectateTarget == null) return@safeListener
            val packet = event.packet
            if (packet is SPacketAnimation && packet.animationType == 0) {
                player.swingArm(EnumHand.MAIN_HAND)
            }
        }

        safeListener<ConnectionEvent.Disconnect> {
            if (spectating) disableSpectating()
        }

        safeListener<ConnectionEvent.Connect> {
            if (spectating) disableSpectating()
        }
    }
}
