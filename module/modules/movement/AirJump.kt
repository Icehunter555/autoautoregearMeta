package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module

object AirJump : Module(
    name = "AirJump",
    category = Category.MOVEMENT,
    description = "jump when in air",
    priority = 1010
) {
    private val verticalSpeed by setting("Vertical Speed", 5.0f, 1.0f..20.0f, 1.0f)
    private val horizontalControl by setting("Horizontal Control", 10.0f, 1.0f..20.0f, 1.0f)
    
    private var hasJumped = false

    private fun shouldAllow(): Boolean {
        return !ElytraFly.isEnabled && !Speed.isEnabled
    }

    init {
        onEnable {
            hasJumped = false
        }

        onDisable {
            hasJumped = false
        }

        listener<TickEvent.Post> {
            if (shouldAllow() && !player.capabilities.isCreativeMode) {
                player.capabilities.isFlying = false
                player.jumpMovementFactor = horizontalControl / 100.0f
                
                if (mc.gameSettings.keyBindJump.isKeyDown) {
                    if (!hasJumped && !player.onGround) {
                        player.motionY = (verticalSpeed / 10.0f).toDouble()
                        hasJumped = true
                    } else if (hasJumped && player.onGround) {
                        hasJumped = false
                    }
                }
            }
        }
    }
}
