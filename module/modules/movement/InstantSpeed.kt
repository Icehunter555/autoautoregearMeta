package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.init.MobEffects

object InstantSpeed : Module(
    name = "InstantSpeed",
    alias = arrayOf("NoAccel", "NoAcceleration"),
    category = Category.MOVEMENT,
    description = "Move with max velocity instantly",
    priority = 1008
) {
    private val stopInLiquid by setting("Stop In Liquid", true)
    private val groundOnly by setting("Ground Only", true)
    private val speedBase by setting("Speed Base", 0.2873, 0.0..1.0, 1.0E-4)
    private val speedBoost by setting("Speed Boost", true)

    private fun SafeClientEvent.getSpeed(): Double {
        var base = speedBase
        if (!speedBoost) return base
        player.getActivePotionEffect(MobEffects.SPEED)?.let {
            base *= 1.0 + 0.2 * (it.amplifier + 1)
        }
        return base
    }

    private fun SafeClientEvent.forward(speed: Double): DoubleArray {
        val movement = player.movementInput ?: return doubleArrayOf(0.0, 0.0)
        var forward = movement.moveForward
        var side = movement.moveStrafe
        var yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * mc.renderPartialTicks
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += (if (forward > 0.0f) -45.0f else 45.0f)
            } else if (side < 0.0f) {
                yaw += (if (forward > 0.0f) 45.0f else -45.0f)
            }
            side = 0.0f
            forward = if (forward > 0.0f) 1.0f else -1.0f
        }
        val sin = Math.sin(Math.toRadians((yaw + 90.0f).toDouble()))
        val cos = Math.cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val posX = forward.toDouble() * speed * cos + side.toDouble() * speed * sin
        val posZ = forward.toDouble() * speed * sin - side.toDouble() * speed * cos
        return doubleArrayOf(posX, posZ)
    }

    init {
        listener<PlayerMoveEvent.Pre> { event ->
            if (stopInLiquid && (player.isInLava || player.isInWater)) return@listener
            if (groundOnly && !player.onGround) return@listener

            val speed = forward(getSpeed())
            event.x = speed[0]
            event.z = speed[1]
        }
    }
}
