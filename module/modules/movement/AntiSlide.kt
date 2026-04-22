package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.init.Blocks

object AntiSlide : Module(
    name = "AntiSlide",
    category = Category.MOVEMENT,
    description = "Stops sliding on ice and from movement",
    priority = 1010
) {
    private val onGround by setting("On Ground", true)

    override fun getHudInfo(): String {
        return if (onGround) "Ground" else "Ice"
    }

    private fun setIceSlipperiness(value: Float) {
        Blocks.ICE.setDefaultSlipperiness(value)
        Blocks.FROSTED_ICE.setDefaultSlipperiness(value)
        Blocks.PACKED_ICE.setDefaultSlipperiness(value)
    }

    init {
        onDisable {
            setIceSlipperiness(0.98f)
        }

        listener<TickEvent.Post> {
            if (onGround && player.movementInput.moveForward == 0.0f && player.movementInput.moveStrafe == 0.0f) {
                player.motionX = 0.0
                player.motionZ = 0.0
            }
            if (!player.isRiding) {
                setIceSlipperiness(0.6f)
            } else {
                setIceSlipperiness(0.98f)
            }
        }
    }
}
