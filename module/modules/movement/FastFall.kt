package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.accessor.isInWeb
import dev.wizard.meta.util.interfaces.DisplayEnum

object FastFall : Module(
    name = "FastFall",
    category = Category.MOVEMENT,
    description = "Makes you fall faster",
    priority = 50
) {
    private val mode by setting("Mode", Mode.MOTION)
    private val fallSpeed by setting("Fall Speed", 6.0, 0.1..10.0, 0.1)
    private val fallDistance by setting("Max Fall Distance", 2, 0..20, 1)

    private var timering = false
    private var motioning = false

    override fun getHudInfo(): String {
        return "${mode.displayName}, $fallSpeed, $fallDistance"
    }

    private fun reset() {
        if (timering) {
            TimerManager.resetTimer(this)
            timering = false
        }
        motioning = false
    }

    init {
        listener<TickEvent.Post> {
            if (player.onGround || player.isElytraFlying || player.isInLava || player.isInWater || player.isInWeb || player.fallDistance < fallDistance.toFloat() || player.capabilities.isFlying) {
                reset()
                return@listener
            }

            when (mode) {
                Mode.MOTION -> {
                    player.motionY -= fallSpeed
                    motioning = true
                }
                Mode.TIMER -> {
                    TimerManager.modifyTimer(this, 50.0f / (fallSpeed.toFloat() * 2.0f))
                    timering = true
                }
            }
        }
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        MOTION("Motion"), TIMER("Timer")
    }
}
