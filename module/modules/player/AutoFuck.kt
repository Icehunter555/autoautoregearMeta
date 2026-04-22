package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding

object AutoFuck : Module(
    "AutoFuck",
    category = Category.PLAYER,
    description = "for 3uer"
) {
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 100, 10..5000, 10))
    private val timer = TickTimer()
    private var sneaking = false

    override fun getHudInfo(): String = delay.toString()

    init {
        safeListener<TickEvent.Post> {
            if (timer.tickAndReset(delay.toLong(), TimeUnit.MILLISECONDS)) {
                sneaking = !sneaking
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, sneaking)
            }
        }

        onDisable {
            SafeClientEvent.instance?.let {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, GameSettings.isKeyDown(it.mc.gameSettings.keyBindSneak))
            }
        }
    }
}
