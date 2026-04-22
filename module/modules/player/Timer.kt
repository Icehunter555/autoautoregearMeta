package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atTrue

object Timer : Module(
    "Timer",
    category = Category.PLAYER,
    description = "Changes your client tick speed",
    modulePriority = 500
) {
    private val pauseOnMove by setting(this, BooleanSetting(settingName("Pause On Move"), false))
    private val pauseOnSteady by setting(this, BooleanSetting(settingName("Pause On Steady"), false))
    private val slow by setting(this, BooleanSetting(settingName("Slow Mode"), false))
    private val tickNormal by setting(this, FloatSetting(settingName("Tick N"), 2.0f, 1.0f..10.0f, 0.1f, slow.atFalse()))
    private val tickSlow by setting(this, FloatSetting(settingName("Tick S"), 8.0f, 1.0f..10.0f, 0.1f, slow.atTrue()))

    init {
        onDisable {
            TimerManager.resetTimer(this)
        }

        listener<RunGameLoopEvent.Start> {
            val inputting = MovementUtils.isInputting(jump = true)
            if ((pauseOnMove && inputting) || (pauseOnSteady && !inputting)) {
                TimerManager.resetTimer(this)
                return@listener
            }

            val multiplier = if (!slow) tickNormal else tickSlow / 10.0f
            TimerManager.modifyTimer(this, 50.0f / multiplier)
        }
    }
}
