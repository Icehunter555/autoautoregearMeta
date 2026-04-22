package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module

object KeepSprint : Module(
    name = "KeepSprint",
    category = Category.MOVEMENT,
    description = "keep sprinting forever!",
    priority = 1010
) {
    private var prev: Triple<Boolean, Double, Double>? = null

    @JvmStatic
    fun onHitPre() {
        if (!isEnabled) return
        SafeClientEvent.instance?.let {
            prev = Triple(it.player.isSprinting, it.player.motionX, it.player.motionZ)
        }
    }

    @JvmStatic
    fun onHitPost() {
        if (!isEnabled) return
        SafeClientEvent.instance?.let { event ->
            prev?.let {
                event.player.setSprinting(it.first)
                event.player.motionX = it.second
                event.player.motionZ = it.third
            }
        }
    }
}
