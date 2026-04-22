package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerTravelEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.world.getGroundLevel
import net.minecraft.network.play.server.SPacketPlayerPosLook

object ReverseStep : Module(
    name = "ReverseStep",
    category = Category.MOVEMENT,
    description = "Walks down edge of block faster",
    priority = 1010
) {
    private val height by setting("Height", 2.0f, 0.25f..12.0f, 0.1f)
    private val speed by setting("Speed", 1.0f, 0.1f..8.0f, 0.1f)
    private val timer = TickTimer()

    private fun SafeClientEvent.shouldRun(): Boolean {
        if (mc.gameSettings.keyBindSneak.isKeyDown) return false
        if (mc.gameSettings.keyBindJump.isKeyDown) return false
        if (player.isElytraFlying) return false
        if (player.capabilities.isFlying) return false
        if (player.isOnLadder) return false
        if (EntityUtils.isInOrAboveLiquid(player)) return false
        if (!player.onGround) return false
        if (player.motionY < -0.08 || player.motionY > 0.0) return false
        if (!timer.tick(3000L)) return false
        return checkGroundLevel()
    }

    private fun SafeClientEvent.checkGroundLevel(): Boolean {
        val currentHeight = height.toDouble()
        val dist1 = player.posY - world.getGroundLevel(player)
        if (dist1 in 0.25..currentHeight) return true

        val dist2 = player.posY - world.getGroundLevel(player.entityBoundingBox.offset(player.motionX, 0.0, player.motionZ))
        if (dist2 in 0.25..currentHeight) return true

        val dist3 = player.posY - world.getGroundLevel(player.entityBoundingBox.offset(player.motionX * 2.0, 0.0, player.motionZ * 2.0))
        if (dist3 in 0.25..currentHeight) return true

        return false
    }

    init {
        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) {
                timer.reset()
            }
        }

        listener<PlayerTravelEvent>(100) {
            if (shouldRun()) {
                player.motionY -= speed.toDouble()
                Speed.resetReverseStep()
            }
        }
    }
}
