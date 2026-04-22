package dev.wizard.meta.module.modules.client

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.font.GlyphCache
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.threads.onMainThread
import java.awt.Font

object CustomFont : Module(
    "Font",
    category = Category.CLIENT,
    description = "Custom font settings for the client",
    alwaysEnabled = true
) {
    val availableFonts = listOf("orbitron", "underdog", "winkySans", "gidole", "geo", "comic", "queen", "jetbrains", "monofur")
    val fontToUse = setting(this, EnumSetting(settingName("Font"), FontToUse.ORBITRON))
    val overrideMinecraft by setting(this, BooleanSetting(settingName("Override Minecraft"), false))
    private val sizeSetting = setting(this, FloatSetting(settingName("Size"), 1.0f, 0.5f..2.0f, 0.05f))
    private val charGapSetting = setting(this, FloatSetting(settingName("Char Gap"), 0.0f, -10.0f..10.0f, 0.5f))
    private val lineSpaceSetting = setting(this, FloatSetting(settingName("Line Space"), 0.0f, -10.0f..10.0f, 0.05f))
    private val baselineOffsetSetting = setting(this, FloatSetting(settingName("Baseline Offset"), 0.0f, -10.0f..10.0f, 0.05f))
    private val lodBiasSetting = setting(this, FloatSetting(settingName("Lod Bias"), 0.0f, -10.0f..10.0f, 0.05f))

    init {
        safeListener<TickEvent.Post>(alwaysListening = true) {
            mc.fontRenderer.FONT_HEIGHT = if (overrideMinecraft) MathUtilKt.ceilToInt(MainFontRenderer.height) else 9
        }

        fontToUse.valueListeners.add { prev, it ->
            if (prev == it) return@add
            GlyphCache.delete(Font(prev.name, Font.PLAIN, 64))
            onMainThread {
                MainFontRenderer.reloadFonts()
            }
        }
    }

    val size: Float get() = sizeSetting.value * 0.140625f
    val charGap: Float get() = charGapSetting.value * 0.5f
    val lineSpace: Float get() = size * (lineSpaceSetting.value * 0.05f + 0.75f)
    val lodBias: Float get() = lodBiasSetting.value * 0.25f - 0.5375f
    val baselineOffset: Float get() = baselineOffsetSetting.value * 2.0f - 9.5f

    enum class FontToUse(override val displayName: CharSequence) : DisplayEnum {
        ORBITRON("Orbitron"), UNDERDOG("Underdog"), WINKYSANS("WinkySans"), GIDOLE("Gidole"), GEO("Geo"),
        COMIC("Comic"), QUEEN("Queen"), JETBRAINS("Jetbrains"), MONOFUR("Monofur"), GOLDMAN("Goldman"),
        LEXEND("Lexend"), MACONDO("Macondo"), STORY("StoryScript"), TEKTUR("Tektur"),
        MINECRAFT("Minecraft"), GHRATHE("GhRathe"), FOR3UER("3uer")
    }
}
