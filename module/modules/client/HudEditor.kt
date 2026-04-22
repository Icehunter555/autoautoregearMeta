package dev.wizard.meta.module.modules.client

import dev.wizard.meta.event.events.ShutdownEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.gui.hudgui.TrollHudGui
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.threads.onMainThreadSafe

object HudEditor : Module(
    "HudEditor",
    category = Category.CLIENT,
    description = "Edits the Hud"
) {
    val enableHud by setting(this, BooleanSetting(settingName("Enable Hud"), true))
    val defaultTextMode = setting(this, EnumSetting(settingName("Default Hud Mode"), DefaultTextColorMode.NORMAL))

    private val defaultColorOne = setting(this, ColorSetting(settingName("Default Color 1"), ColorRGB(255, 255, 255), { defaultTextMode.value == DefaultTextColorMode.NORMAL }))
    private val defaultColorTwo = setting(this, ColorSetting(settingName("Default Color 2"), ColorRGB(127, 127, 127), { defaultTextMode.value == DefaultTextColorMode.NORMAL }))

    private val defaultGradientOne = setting(this, ColorSetting(settingName("Default Gradient Color 1"), ColorRGB(255, 255, 255), { defaultTextMode.value == DefaultTextColorMode.GRADIENT }))
    private val defaultGradientTwo = setting(this, ColorSetting(settingName("Default Gradient Color 2"), ColorRGB(255, 140, 180), { defaultTextMode.value == DefaultTextColorMode.GRADIENT }))

    val staticGradientByDefault by setting(this, BooleanSetting(settingName("Static Gradients"), false, { defaultTextMode.value == DefaultTextColorMode.GRADIENT }))
    val defaultGradientSpeed by setting(this, FloatSetting(settingName("Default Gradient Speed"), 1.0f, 0.1f..5.0f, 0.1f, LambdaUtilsKt.and({ defaultTextMode.value == DefaultTextColorMode.GRADIENT }, { !staticGradientByDefault })))

    init {
        onEnable {
            onMainThreadSafe {
                if (mc.currentScreen !is TrollHudGui) {
                    ClickGUI.disable()
                    mc.displayGuiScreen(TrollHudGui)
                    TrollHudGui.onDisplayed()
                }
            }
        }

        onDisable {
            onMainThreadSafe {
                if (mc.currentScreen is TrollHudGui) {
                    mc.displayGuiScreen(null)
                }
            }
        }

        listener<ShutdownEvent> {
            disable()
        }
    }

    val colorOne: ColorRGB get() = when (defaultTextMode.value) {
        DefaultTextColorMode.NORMAL -> defaultColorOne.value
        DefaultTextColorMode.GRADIENT -> defaultGradientOne.value
    }

    val colorTwo: ColorRGB get() = when (defaultTextMode.value) {
        DefaultTextColorMode.NORMAL -> defaultColorTwo.value
        DefaultTextColorMode.GRADIENT -> defaultGradientTwo.value
    }

    enum class DefaultTextColorMode(override val displayName: CharSequence) : DisplayEnum { NORMAL("Normal"), GRADIENT("Gradient") }
}
