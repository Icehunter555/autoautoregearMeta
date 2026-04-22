package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.util.accessor.setBlockHitDelay

object FastBreak : Module(
    "FastBreak",
    category = Category.PLAYER,
    description = "Breaks block faster and nullifies the break delay"
) {
    private val breakDelay by setting(this, IntegerSetting(settingName("Break Delay"), 0, 0..5, 1))

    @JvmStatic
    fun updateBreakDelay() {
        if (INSTANCE.isEnabled) {
            SafeClientEvent.instance?.let {
                it.playerController.setBlockHitDelay(INSTANCE.breakDelay)
            }
        }
    }
}
