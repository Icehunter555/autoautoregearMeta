package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.AnimationFlag
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module

object GuiAnimation : Module(
    "GuiAnimation",
    category = Category.RENDER,
    description = "Animates Minecraft gui",
    enabledByDefault = true
) {
    private val hotbarAnimation = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    init {
        onEnable {
            val safe = SafeClientEvent.instance ?: return@onEnable
            val currentPos = safe.player.inventory.currentItem * 20.0f
            hotbarAnimation.forceUpdate(currentPos, currentPos)
        }
    }

    @JvmStatic
    fun updateHotbar(): Float {
        val currentPos = mc.player?.inventory?.currentItem?.toFloat()?.let { it * 20.0f } ?: 0.0f
        return hotbarAnimation.getAndUpdate(currentPos)
    }
}
