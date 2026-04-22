package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.ResolutionUpdateEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.threads.onMainThread
import org.lwjgl.opengl.Display

object AntiAlias : Module(
    "AntiAlias",
    category = Category.RENDER,
    description = "Enables Antialias"
) {
    private val disableInBackground by setting(this, BooleanSetting(settingName("Disable In Background"), true))
    private val sampleLevel0 = setting(this, FloatSetting(settingName("SSAA Level"), 1.0f, 1.0f..2.0f, 0.05f))

    private var prevSampleLevel = 1.0f

    val sampleLevel: Float
        get() = if (isEnabled && Display.isActive()) sampleLevel0.value else 1.0f

    init {
        onToggle {
            onMainThread {
                mc.resize(mc.displayWidth, mc.displayHeight)
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
            }
        }

        listener<TickEvent.Pre> {
            val level = sampleLevel
            if (level != prevSampleLevel) {
                prevSampleLevel = level
                mc.resize(mc.displayWidth, mc.displayHeight)
                ResolutionUpdateEvent(mc.displayWidth, mc.displayHeight).post()
            }
        }
    }
}
