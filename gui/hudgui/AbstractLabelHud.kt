package dev.wizard.meta.gui.hudgui

import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.modules.client.HudEditor
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.LambdaUtilsKt.and
import dev.wizard.meta.util.LambdaUtilsKt.atFalse
import dev.wizard.meta.util.LambdaUtilsKt.atValue
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.interfaces.Nameable
import dev.wizard.meta.util.math.vector.Vec2d
import dev.wizard.meta.util.text.format
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.util.text.TextFormatting

abstract class AbstractLabelHud(
    name: String,
    alias: Array<out String>,
    category: Category,
    description: String,
    alwaysListening: Boolean,
    enabledByDefault: Boolean,
    config: AbstractConfig<out Nameable>
) : AbstractHudElement(name, alias, category, description, alwaysListening, enabledByDefault, config) {

    override val hudWidth by FrameFloat { displayText.width + 2.0f }
    override val hudHeight by FrameFloat { displayText.height }

    val textColorMode by GuiConfig.setting(this, "Text Color Mode", TextColorMode.DEFAULT)
    val textColorOne by GuiConfig.setting(this, "Text Color 1", ColorRGB(255, 255, 255), visibility = atValue(::textColorMode, TextColorMode.NORMAL))
    val textColorTwo by GuiConfig.setting(this, "Text Color 2", ColorRGB(127, 127, 127), visibility = atValue(::textColorMode, TextColorMode.NORMAL))
    val textGradientOne by GuiConfig.setting(this, "Gradient Color 1", ColorRGB(255, 255, 255), visibility = atValue(::textColorMode, TextColorMode.GRADIENT))
    val textGradientTwo by GuiConfig.setting(this, "Gradient Color 2", ColorRGB(255, 140, 180), visibility = atValue(::textColorMode, TextColorMode.GRADIENT))
    val staticTextGradient by GuiConfig.setting(this, "Static Gradient", false, visibility = atValue(::textColorMode, TextColorMode.GRADIENT))
    val textGradientSpeed by GuiConfig.setting(this, "Gradient Speed", 1.0f, 0.1f..5.0f, 0.1f, visibility = atValue(::textColorMode, TextColorMode.GRADIENT) and atFalse(::staticTextGradient))

    protected val displayText = TextComponent()

    init {
        parallelListener<TickEvent.Post> {
            displayText.clear()
            updateText(this)
            if (displayText.isEmpty() && screen.isVisible) {
                displayText.add(TextFormatting.ITALIC.format(nameAsString))
            }
        }
    }

    protected abstract fun updateText(event: SafeClientEvent)

    override fun renderHud() {
        super.renderHud()
        val textPosX = width * dockingH.multiplier / scale - dockingH.offset
        val textPosY = height * dockingV.multiplier / scale
        displayText.draw(Vec2d(textPosX.toDouble(), textPosY.toDouble()), dockingH = dockingH, dockingV = dockingV)
    }

    protected fun addText(
        text: String,
        color1: ColorRGB? = null,
        color2: ColorRGB? = null,
        secondary: Boolean = false,
        addSpace: Pair<Boolean, Boolean> = false to false
    ) {
        when (textColorMode) {
            TextColorMode.DEFAULT -> {
                when (HudEditor.defaultTextMode) {
                    HudEditor.DefaultTextColorMode.NORMAL -> {
                        val c = color1?.unbox() ?: if (secondary) HudEditor.colorTwo else HudEditor.colorOne
                        displayText.add(text, c)
                    }
                    HudEditor.DefaultTextColorMode.GRADIENT -> {
                        val c1 = color1?.unbox() ?: HudEditor.colorOne
                        val c2 = color2?.unbox() ?: HudEditor.colorTwo
                        val txt = (if (addSpace.first) " " else "") + text + (if (addSpace.second) " " else "")
                        addGradientText(txt, c1, c2, HudEditor.defaultGradientSpeed, !HudEditor.staticGradientByDefault)
                    }
                }
            }
            TextColorMode.NORMAL -> {
                val c = color1?.unbox() ?: if (secondary) textColorTwo else textColorOne
                displayText.add(text, c)
            }
            TextColorMode.GRADIENT -> {
                val c1 = color1?.unbox() ?: textGradientOne
                val c2 = color2?.unbox() ?: textGradientTwo
                val txt = (if (addSpace.first) " " else "") + text + (if (addSpace.second) " " else "")
                addGradientText(txt, c1, c2, textGradientSpeed, staticTextGradient)
            }
        }
    }

    protected fun addTextLine(
        text: String,
        color1: ColorRGB? = null,
        color2: ColorRGB? = null,
        secondary: Boolean = false
    ) {
        when (textColorMode) {
            TextColorMode.DEFAULT -> {
                when (HudEditor.defaultTextMode) {
                    HudEditor.DefaultTextColorMode.NORMAL -> {
                        val c = color1?.unbox() ?: if (secondary) HudEditor.colorTwo else HudEditor.colorOne
                        displayText.addLine(text, c)
                    }
                    HudEditor.DefaultTextColorMode.GRADIENT -> {
                        val c1 = color1?.unbox() ?: HudEditor.colorOne
                        val c2 = color2?.unbox() ?: HudEditor.colorTwo
                        addGradientLine(text, c1, c2, HudEditor.defaultGradientSpeed, HudEditor.staticGradientByDefault)
                    }
                }
            }
            TextColorMode.NORMAL -> {
                val c = color1?.unbox() ?: if (secondary) textColorTwo else textColorOne
                displayText.addLine(text, c)
            }
            TextColorMode.GRADIENT -> {
                val c1 = color1?.unbox() ?: textGradientOne
                val c2 = color2?.unbox() ?: textGradientTwo
                addGradientLine(text, c1, c2, textGradientSpeed, staticTextGradient)
            }
        }
    }

    protected fun addGradientText(
        components: List<String>,
        color1: Int,
        color2: Int,
        gradientSpeed: Float = 1.0f,
        cycleGradient: Boolean = true
    ) {
        if (components.isEmpty()) return
        val totalWidth = components.sumOf { MainFontRenderer.getWidth(it).toDouble() }.toFloat()
        if (totalWidth <= 0.0f) return

        val timeFactor = (System.currentTimeMillis() % 10000L).toFloat() / 10000.0f * gradientSpeed
        var currentPos = 0.0f
        for (text in components) {
            if (text.isEmpty()) continue
            val width = MainFontRenderer.getWidth(text)
            val ratio = (currentPos / totalWidth + timeFactor) % 1.0f
            val color = if (cycleGradient) {
                if (ratio < 0.5f) ColorRGB.lerp(color1, color2, ratio * 2.0f)
                else ColorRGB.lerp(color2, color1, (ratio - 0.5f) * 2.0f)
            } else {
                ColorRGB.lerp(color1, color2, ratio)
            }
            displayText.add(text, color)
            currentPos += width
        }
    }

    protected fun addGradientText(
        text: String,
        color1: Int,
        color2: Int,
        gradientSpeed: Float = 1.0f,
        cycleGradient: Boolean = true
    ) {
        if (text.isEmpty()) return
        val timeFactor = (System.currentTimeMillis() % 10000L).toFloat() / 10000.0f * gradientSpeed
        text.forEachIndexed { index, c ->
            val ratio = if (cycleGradient) {
                (index.toFloat() / text.length.toFloat() + timeFactor) % 1.0f
            } else {
                index.toFloat() / (text.length - 1).coerceAtLeast(1).toFloat()
            }
            val color = if (ratio < 0.5f) ColorRGB.lerp(color1, color2, ratio * 2.0f)
            else ColorRGB.lerp(color2, color1, (ratio - 0.5f) * 2.0f)
            displayText.addNoSeparator(c.toString(), color)
        }
    }

    protected fun addGradientTextWithPositions(
        textParts: Map<String, Float>,
        color1: Int,
        color2: Int,
        gradientSpeed: Float = 1.0f,
        cycleGradient: Boolean = true
    ) {
        val timeFactor = (System.currentTimeMillis() % 10000L).toFloat() / 10000.0f * gradientSpeed
        for ((text, position) in textParts) {
            if (text.isEmpty()) continue
            val ratio = (position + timeFactor) % 1.0f
            val color = if (cycleGradient) {
                if (ratio < 0.5f) ColorRGB.lerp(color1, color2, ratio * 2.0f)
                else ColorRGB.lerp(color2, color1, (ratio - 0.5f) * 2.0f)
            } else {
                ColorRGB.lerp(color1, color2, ratio)
            }
            displayText.add(text, color)
        }
    }

    protected fun addSimpleGradient(
        fullText: String,
        color1: Int,
        color2: Int,
        separator: String? = null
    ) {
        val components = if (separator != null) fullText.split(separator) else fullText.map { it.toString() }
        if (components.isEmpty()) return
        components.forEachIndexed { index, text ->
            if (text.isEmpty()) return@forEachIndexed
            val ratio = if (components.size > 1) index.toFloat() / (components.size - 1) else 0.0f
            val color = ColorRGB.lerp(color1, color2, ratio)
            if (separator != null) {
                displayText.add(text, color)
                if (index < components.size - 1) {
                    displayText.add(separator, color)
                }
            } else {
                displayText.addNoSeparator(text, color)
            }
        }
    }

    protected fun addGradientLine(
        components: List<String>,
        color1: Int,
        color2: Int,
        gradientSpeed: Float = 1.0f,
        cycleGradient: Boolean = true
    ) {
        addGradientText(components, color1, color2, gradientSpeed, cycleGradient)
        displayText.currentLine++
    }

    protected fun addGradientLine(
        text: String,
        color1: Int,
        color2: Int,
        gradientSpeed: Float = 1.0f,
        cycleGradient: Boolean = true
    ) {
        addGradientText(text, color1, color2, gradientSpeed, cycleGradient)
        displayText.currentLine++
    }

    protected fun addGradientLineWithPositions(
        textParts: Map<String, Float>,
        color1: Int,
        color2: Int,
        gradientSpeed: Float = 1.0f,
        cycleGradient: Boolean = true
    ) {
        addGradientTextWithPositions(textParts, color1, color2, gradientSpeed, cycleGradient)
        displayText.currentLine++
    }

    protected fun addSimpleGradientLine(
        fullText: String,
        color1: Int,
        color2: Int,
        separator: String? = null
    ) {
        addSimpleGradient(fullText, color1, color2, separator)
        displayText.currentLine++
    }

    enum class TextColorMode(override val displayName: CharSequence) : DisplayEnum {
        DEFAULT("Default"),
        NORMAL("Normal"),
        GRADIENT("Gradient")
    }
}
