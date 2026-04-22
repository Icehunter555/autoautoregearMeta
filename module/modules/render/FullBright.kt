package dev.wizard.meta.module.modules.render

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.util.LambdaUtilsKt

object FullBright : Module(
    "FullBright",
    category = Category.RENDER,
    description = "Makes everything brighter!",
    alwaysListening = true
) {
    private val gamma by setting(this, FloatSetting(settingName("Gamma"), 12.0f, 5.0f..15.0f, 0.5f))
    private val transitionLength by setting(this, FloatSetting(settingName("Transition Length"), 3.0f, 0.0f..10.0f, 0.5f))
    private var oldValue by setting(this, FloatSetting(settingName("Old Value"), 1.0f, 0.0f..1.0f, 0.1f, LambdaUtilsKt.BOOLEAN_SUPPLIER_FALSE))

    private val disableTimer = TickTimer()

    override fun getHudInfo(): String = "%.2f".format(gamma)

    private var gammaSetting: Float
        get() = mc.gameSettings.gammaSetting
        set(value) {
            mc.gameSettings.gammaSetting = value
        }

    init {
        onEnable {
            oldValue = gammaSetting
        }

        onDisable {
            disableTimer.reset()
        }

        safeListener<TickEvent.Post> {
            if (isEnabled) {
                transition(gamma)
                alwaysListening = true
            } else if (isDisabled && gammaSetting != oldValue && !disableTimer.tick((transitionLength * 1000.0f).toLong())) {
                transition(oldValue)
            } else {
                alwaysListening = false
            }
        }
    }

    private fun transition(target: Float) {
        val current = gammaSetting
        if (current !in 0.0f..15.0f) {
            gammaSetting = target
        } else {
            if (current == target) return
            val amount = getTransitionAmount()
            gammaSetting = if (current < target) {
                (current + amount).coerceAtMost(target)
            } else {
                (current - amount).coerceAtLeast(target)
            }
        }
    }

    private fun getTransitionAmount(): Float {
        if (transitionLength == 0.0f) return 15.0f
        return Math.abs(gamma - oldValue) / (transitionLength * 20.0f)
    }
}
