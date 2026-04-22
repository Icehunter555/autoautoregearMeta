package dev.wizard.meta.gui.hudgui.elements.hud

import dev.fastmc.common.TimeUnit
import dev.fastmc.common.collection.FastIntMap
import dev.fastmc.common.sort.ObjectIntrosort
import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.LambdaUtilsKt.atValue
import dev.wizard.meta.util.delegate.AsyncCachedValue
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.extension.sumOfFloat
import dev.wizard.meta.util.state.TimedFlag
import dev.wizard.meta.util.threads.runSafeSuspend
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.text.TextFormatting
import kotlin.math.max

object ActiveModules : HudElement("Active Modules", category = Category.HUD, description = "List of enabled modules", enabledByDefault = true) {

    private val mode by setting(this, "Mode", Mode.LEFT_TAG)
    private val sortingMode by setting(this, "Sorting Mode", SortingMode.LENGTH)
    private val showInvisible by setting(this, "Show Invisible", false)
    private val showRender by setting(this, "Show Render", false)
    private val bindOnly by setting(this, "Bind Only", true, visibility = { !showInvisible })
    private val gradientMode by setting(this, "Gradient Mode", GradientMode.ANIMATED_GRADIENT)
    var color1 by setting(this, "Primary Color", ColorRGB(20, 235, 20), visibility = { gradientMode != GradientMode.RAINBOW })
    val color2 by setting(this, "Secondary Color", ColorRGB(20, 235, 20), visibility = { gradientMode != GradientMode.RAINBOW })
    private val gradientSpeed by setting(this, "Animation Speed", 1.0f, 0.1f..5.0f, 0.1f)
    private val saturation by setting(this, "Saturation", 0.8f, 0.0f..1.0f, 0.01f, visibility = atValue(::gradientMode, GradientMode.RAINBOW))
    private val brightness by setting(this, "Brightness", 1.0f, 0.0f..1.0f, 0.01f, visibility = atValue(::gradientMode, GradientMode.RAINBOW))

    override val hudWidth by FrameFloat {
        ModuleManager.modules.asSequence()
            .filter { !it.isDevOnly }
            .maxOfOrNull {
                val flag = toggleMap[it.id]
                if (flag?.value == true) getTextLine(it).getWidth() + 4.0f else 20.0f
            }?.let { max(it, 20.0f) } ?: 20.0f
    }

    override val hudHeight by FrameFloat {
        val height = toggleMap.values.sumOfFloat { getDisplayHeight(it) }
        max(height, 20.0f)
    }

    private val textLineMap = Int2ObjectOpenHashMap<TextComponent.TextLine>()
    private var lastSorted = emptyArray<SortingPair>()
    private var prevToggleMap = FastIntMap<ModuleToggleFlag>()
    private val toggleMap: FastIntMap<ModuleToggleFlag> by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        val newMap = FastIntMap<ModuleToggleFlag>()
        ModuleManager.modules.filter { !it.isDevOnly }.forEach {
            newMap.set(it.id, prevToggleMap.get(it.id) ?: ModuleToggleFlag(it))
        }
        prevToggleMap = newMap
        newMap
    }

    init {
        parallelListener<TickEvent.Post> {
            toggleMap.entries.forEach { (id, flag) ->
                flag.update()
                if (getProgress(flag) > 0.0f) {
                    textLineMap[id] = newTextLine(flag.module)
                }
            }
        }

        relativePosX = -2.0f
        relativePosY = 2.0f
        dockingH = HAlign.RIGHT
    }

    override fun renderHud() {
        super.renderHud()
        GlStateManager.pushMatrix()
        GlStateManager.translate(width / scale * dockingH.multiplier, 0.0f, 0.0f)
        if (dockingV == VAlign.BOTTOM) {
            GlStateManager.translate(0.0f, height / scale - (MainFontRenderer.height + 2.0f), 0.0f)
        } else if (dockingV == VAlign.TOP) {
            GlStateManager.translate(0.0f, -1.0f, 0.0f)
        }
        if (dockingH == HAlign.LEFT) {
            GlStateManager.translate(-1.0f, 0.0f, 0.0f)
        }

        when (mode) {
            Mode.LEFT_TAG -> if (dockingH == HAlign.LEFT) GlStateManager.translate(2.0f, 0.0f, 0.0f)
            Mode.RIGHT_TAG -> if (dockingH == HAlign.RIGHT) GlStateManager.translate(-2.0f, 0.0f, 0.0f)
            else -> {}
        }

        drawModuleList()
        GlStateManager.popMatrix()
    }

    private fun drawModuleList() {
        val sortArray = makeKeyPair(ModuleManager.modules, lastSorted)
        lastSorted = sortArray
        sortArray.forEach { it.update() }
        ObjectIntrosort.sort(sortArray)

        val totalModules = sortArray.count { toggleMap.get(it.module.id)?.value == true }
        var visibleIndex = 0
        val timeFactor = (System.currentTimeMillis() % 10000L).toFloat() / 10000.0f * gradientSpeed

        for (pair in sortArray) {
            val module = pair.module
            val timedFlag = toggleMap.get(module.id) ?: continue
            if (getProgress(timedFlag) <= 0.0f) continue

            val ratio = visibleIndex.toFloat() / max(totalModules, 1).toFloat()
            val color = when (gradientMode) {
                GradientMode.RAINBOW -> {
                    val hue = (ratio + timeFactor) % 1.0f
                    ColorRGB.fromHSB(hue, saturation, brightness)
                }
                GradientMode.STATIC_GRADIENT -> ColorRGB.lerp(color1, color2, ratio)
                GradientMode.ANIMATED_GRADIENT -> {
                    val lerpProgress = (ratio + timeFactor) % 1.0f
                    if (lerpProgress < 0.5f) ColorRGB.lerp(color1, color2, lerpProgress * 2.0f)
                    else ColorRGB.lerp(color2, color1, (lerpProgress - 0.5f) * 2.0f)
                }
            }

            drawModule(module, color)
            visibleIndex++
        }
    }

    private fun drawModule(module: AbstractModule, color: Int) {
        val timedFlag = toggleMap.get(module.id) ?: return
        val progress = getProgress(timedFlag)
        if (progress <= 0.0f) return

        GlStateManager.pushMatrix()
        val textLine = newTextLine(module, color)
        val textWidth = textLine.getWidth()
        val animationXOffset = textWidth * dockingH.offset * (1.0f - progress)
        val stringPosX = textWidth * dockingH.multiplier
        val margin = 2.0f * dockingH.offset
        var yOffset = getDisplayHeight(timedFlag)

        GlStateManager.translate(animationXOffset - margin - stringPosX, 0.0f, 0.0f)

        when (mode) {
            Mode.LEFT_TAG -> {
                RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, ClickGUI.backGround)
                RenderUtils2D.drawRectFilled(-4.0f, 0.0f, -2.0f, yOffset, color)
            }
            Mode.RIGHT_TAG -> {
                RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, ClickGUI.backGround)
                RenderUtils2D.drawRectFilled(textWidth + 2.0f, 0.0f, textWidth + 4.0f, yOffset, color)
            }
            Mode.FRAME -> {
                RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, ClickGUI.backGround)
            }
        }

        textLine.drawLine(progress, HAlign.LEFT)
        if (dockingV == VAlign.BOTTOM) yOffset *= -1.0f
        GlStateManager.popMatrix()
        GlStateManager.translate(0.0f, yOffset, 0.0f)
    }

    private fun getTextLine(module: AbstractModule): TextComponent.TextLine {
        return textLineMap.getOrPut(module.id) { newTextLine(module) }
    }

    private fun newTextLine(module: AbstractModule, color: Int = ClickGUI.primary): TextComponent.TextLine {
        val textLine = TextComponent.TextLine(" ")
        textLine.add(TextComponent.TextElement(module.nameAsString, color))
        val info = module.hudInfo
        if (info.isNotBlank() && module.showInfo) {
            textLine.add(TextComponent.TextElement("${TextFormatting.GRAY}[$info${TextFormatting.GRAY}]", ColorRGB(255, 255, 255)))
        }
        if (dockingH == HAlign.RIGHT) textLine.reverse()
        return textLine
    }

    private fun getDisplayHeight(timedFlag: TimedFlag<Boolean>): Float {
        return (MainFontRenderer.height + 2.0f) * getProgress(timedFlag)
    }

    private fun getProgress(timedFlag: TimedFlag<Boolean>): Float {
        return if (timedFlag.value) Easing.OUT_CUBIC.inc(Easing.toDelta(timedFlag.lastUpdateTime, 300L))
        else Easing.IN_CUBIC.dec(Easing.toDelta(timedFlag.lastUpdateTime, 300L))
    }

    private fun getState(module: AbstractModule): Boolean {
        return module.isEnabled && (showInvisible || (module.isVisible && (!bindOnly || !module.bind.value.isEmpty()) && (showRender || module.category != Category.RENDER)))
    }

    private fun makeKeyPair(modules: List<AbstractModule>, old: Array<SortingPair>?): Array<SortingPair> {
        val filtered = modules.filter { !it.isDevOnly }
        if (old != null && filtered.size == old.size) return old
        return Array(filtered.size) { SortingPair(filtered[it]) }
    }

    private enum class Mode {
        LEFT_TAG, RIGHT_TAG, FRAME
    }

    private enum class GradientMode {
        RAINBOW, STATIC_GRADIENT, ANIMATED_GRADIENT
    }

    private class ModuleToggleFlag(val module: AbstractModule) : TimedFlag<Boolean>(INSTANCE.getState(module)) {
        fun update() {
            value = INSTANCE.getState(module)
        }
    }

    private enum class SortingMode(override val displayName: CharSequence, val keySelector: (AbstractModule) -> Comparable<*>) : DisplayEnum {
        LENGTH("Length", { -INSTANCE.getTextLine(it).getWidth() }),
        ALPHABET("Alphabet", { it.nameAsString }),
        CATEGORY("Category", { it.category.ordinal })
    }

    private data class SortingPair(val module: AbstractModule, var key: Comparable<*> = INSTANCE.sortingMode.keySelector(module)) : Comparable<SortingPair> {
        fun update() {
            key = INSTANCE.sortingMode.keySelector(module)
        }

        override fun compareTo(other: SortingPair): Int {
            return (key as Comparable<Any>).compareTo(other.key)
        }
    }
}
