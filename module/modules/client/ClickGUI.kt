package dev.wizard.meta.module.modules.client

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.events.ShutdownEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.gui.clickgui.TrollClickGui
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.threads.onMainThreadSafe

object ClickGUI : Module(
    "ClickGui",
    category = Category.CLIENT,
    description = "GUI",
    alwaysListening = true
) {
    private val scaleSetting = setting(this, IntegerSetting(settingName("Scale"), 100, 50..400, 5))
    val backGroundBlur by setting(this, FloatSetting(settingName("Background Blur"), 0.0f, 0.0f..1.0f, 0.05f))
    val windowOutline by setting(this, BooleanSetting(settingName("Window Outline"), true))
    val titleBar by setting(this, BooleanSetting(settingName("Title Bar"), false))
    val windowBlurPass by setting(this, IntegerSetting(settingName("Window Blur Pass"), 2, 0..10, 1))
    val xMargin by setting(this, FloatSetting(settingName("X Margin"), 4.0f, 0.0f..10.0f, 0.5f))
    val yMargin by setting(this, FloatSetting(settingName("Y Margin"), 1.0f, 0.0f..10.0f, 0.5f))
    val darkness by setting(this, FloatSetting(settingName("Darkness"), 0.25f, 0.0f..1.0f, 0.05f))
    val fadeInTime by setting(this, FloatSetting(settingName("Fade In Time"), 0.4f, 0.0f..1.0f, 0.05f))
    val fadeOutTime by setting(this, FloatSetting(settingName("Fade Out Time"), 0.4f, 0.0f..1.0f, 0.05f))
    val primarySetting by setting(this, ColorSetting(settingName("Primary Color"), ColorRGB(255, 140, 180, 220)))
    val backgroundSetting by setting(this, ColorSetting(settingName("Background Color"), ColorRGB(40, 32, 36, 160)))
    val radius0 by setting(this, FloatSetting(settingName("Radius"), 1.7f, 0.3f..2.9f, 0.1f))
    val line by setting(this, BooleanSetting(settingName("Title Line"), false, { !titleBar }))
    val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 1.0f, 0.5f..3.0f, 0.5f, { !titleBar && line }))
    val radius: Float get() = radius0 / 10f

    private val textSetting by setting(this, ColorSetting(settingName("Text Color"), ColorRGB(255, 250, 253, 255)))
    private val aHover by setting(this, IntegerSetting(settingName("Hover Alpha"), 32, 0..255, 1))
    var animatedTitle by setting(this, BooleanSetting(settingName("Animated Title"), true))
    val roundSegments by setting(this, IntegerSetting(settingName("Rounding Segments"), 20, 1..60, 1))

    private var prevScale = 1.0f
    private var scale = 1.0f
    private val settingTimer = TickTimer()
    val scaleFactor by FrameFloat {
        (prevScale + (scale - prevScale) * mc.renderPartialTicks) * 2.0f
    }

    init {
        onEnable {
            onMainThreadSafe {
                if (mc.currentScreen !is TrollClickGui) {
                    HudEditor.disable()
                    mc.displayGuiScreen(TrollClickGui)
                    TrollClickGui.onDisplayed()
                }
            }
        }

        onDisable {
            onMainThreadSafe {
                if (mc.currentScreen is TrollClickGui) {
                    mc.displayGuiScreen(null)
                }
            }
        }

        listener<ShutdownEvent> {
            disable()
        }

        safeParallelListener<TickEvent.Post> {
            prevScale = scale
            if (settingTimer.tick(500L)) {
                val roundedScale = (Math.rint(scaleSetting.value.toDouble() / 100.0 / 0.1) * 0.1).toFloat()
                val diff = scale - roundedScale
                if (diff < -0.025) {
                    scale += 0.025f
                } else if (diff > 0.025) {
                    scale -= 0.025f
                } else {
                    scale = roundedScale
                }
            }
        }

        (bind.value as Bind).bind = 21 // Y key
        scaleSetting.listeners.add { settingTimer.reset() }

        scale = scaleSetting.value.toFloat() / 100.0f
        prevScale = scale
    }

    val idle: ColorRGB get() = if (primarySetting.lightness < 0.9f) ColorRGB(255, 255, 255, 0) else ColorRGB(0, 0, 0, 0)
    val hover: ColorRGB get() = idle.alpha(aHover)
    val click: ColorRGB get() = idle.alpha(aHover * 2)
    val backGround: ColorRGB get() = backgroundSetting
    val text: ColorRGB get() = textSetting
    val primary: ColorRGB get() = primarySetting

    fun resetScale() {
        scaleSetting.value = 100
        prevScale = 1.0f
        scale = 1.0f
    }

    private val roundedScale: Float get() = (Math.rint(scaleSetting.value.toDouble() / 100.0 / 0.1) * 0.1).toFloat()
}
