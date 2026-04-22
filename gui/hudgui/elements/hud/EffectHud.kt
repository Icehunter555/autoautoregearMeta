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
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.delegate.AsyncCachedValue
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.extension.sumOfFloat
import dev.wizard.meta.util.state.TimedFlag
import dev.wizard.meta.util.threads.runSafeSuspend
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import kotlin.math.max

object EffectHud : HudElement("EffectHud", category = Category.TEXT, description = "Displays active effects") {

    private val sortingMode by setting(this, "Sorting Mode", SortingMode.LENGTH)
    private val gradientMode by setting(this, "Gradient Mode", GradientMode.ANIMATED_GRADIENT)
    var color1 by setting(this, "Primary Color", ColorRGB(20, 235, 20))
    val color2 by setting(this, "Secondary Color", ColorRGB(20, 235, 20))
    private val gradientSpeed by setting(this, "Animation Speed", 1.0f, 0.1f..5.0f, 0.1f)
    private val durationColor by setting(this, "Duration Color", ColorRGB(150, 150, 150))

    override val hudWidth by FrameFloat {
        val effects = mc.player?.activePotionEffects ?: return@FrameFloat 20.0f
        effects.maxOfOrNull {
            val potionId = Potion.getIdFromPotion(it.potion)
            if (toggleMap.get(potionId)?.value == true) createTextLine(it).getWidth() + 4.0f else 20.0f
        }?.let { max(it, 20.0f) } ?: 20.0f
    }

    override val hudHeight by FrameFloat {
        val height = toggleMap.values.sumOfFloat { getDisplayHeight(it) }
        max(height, 20.0f)
    }

    private val textLineMap = Int2ObjectOpenHashMap<TextComponent.TextLine>()
    private var lastSorted = emptyArray<SortingPair>()
    private var prevToggleMap = FastIntMap<EffectToggleFlag>()
    private val toggleMap: FastIntMap<EffectToggleFlag> by AsyncCachedValue(1L, TimeUnit.SECONDS) {
        val newMap = FastIntMap<EffectToggleFlag>()
        mc.player?.activePotionEffects?.forEach {
            val potionId = Potion.getIdFromPotion(it.potion)
            newMap.set(potionId, prevToggleMap.get(potionId) ?: EffectToggleFlag(it))
        }
        prevToggleMap = newMap
        newMap
    }

    init {
        parallelListener<TickEvent.Post> {
            val activeEffects = player.activePotionEffects
            toggleMap.entries.forEach { (id, flag) ->
                val effect = activeEffects.find { Potion.getIdFromPotion(it.potion) == id }
                flag.update(effect)
                if (getProgress(flag) > 0.0f && effect != null) {
                    textLineMap[id] = createTextLine(effect)
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

        drawEffectList()
        GlStateManager.popMatrix()
    }

    private fun drawEffectList() {
        val effects = mc.player?.activePotionEffects ?: return
        val sortArray = makeKeyPair(effects, lastSorted)
        lastSorted = sortArray
        sortArray.forEach { it.update() }
        ObjectIntrosort.sort(sortArray)

        val totalEffects = sortArray.count {
            val potionId = Potion.getIdFromPotion(it.effect.potion)
            toggleMap.get(potionId)?.value == true
        }
        var visibleIndex = 0
        val timeFactor = (System.currentTimeMillis() % 10000L).toFloat() / 10000.0f * gradientSpeed

        for (pair in sortArray) {
            val effect = pair.effect
            val potionId = Potion.getIdFromPotion(effect.potion)
            val timedFlag = toggleMap.get(potionId) ?: continue
            if (getProgress(timedFlag) <= 0.0f) continue

            val ratio = visibleIndex.toFloat() / max(totalEffects, 1).toFloat()
            val color = when (gradientMode) {
                GradientMode.STATIC_GRADIENT -> ColorRGB.lerp(color1, color2, ratio)
                GradientMode.ANIMATED_GRADIENT -> {
                    val lerpProgress = (ratio + timeFactor) % 1.0f
                    if (lerpProgress < 0.5f) ColorRGB.lerp(color1, color2, lerpProgress * 2.0f)
                    else ColorRGB.lerp(color2, color1, (lerpProgress - 0.5f) * 2.0f)
                }
            }

            drawEffect(effect, color)
            visibleIndex++
        }
    }

    private fun drawEffect(effect: PotionEffect, color: Int) {
        val potionId = Potion.getIdFromPotion(effect.potion)
        val timedFlag = toggleMap.get(potionId) ?: return
        val progress = getProgress(timedFlag)
        if (progress <= 0.0f) return

        GlStateManager.pushMatrix()
        val textLine = createTextLine(effect, color)
        val textWidth = textLine.getWidth()
        val animationXOffset = textWidth * dockingH.offset * (1.0f - progress)
        val stringPosX = textWidth * dockingH.multiplier
        val margin = 2.0f * dockingH.offset
        var yOffset = getDisplayHeight(timedFlag)

        GlStateManager.translate(animationXOffset - margin - stringPosX, 0.0f, 0.0f)
        RenderUtils2D.drawRectFilled(-2.0f, 0.0f, textWidth + 2.0f, yOffset, ClickGUI.backGround)

        textLine.drawLine(progress, HAlign.LEFT)
        if (dockingV == VAlign.BOTTOM) yOffset *= -1.0f
        GlStateManager.popMatrix()
        GlStateManager.translate(0.0f, yOffset, 0.0f)
    }

    private fun createTextLine(effect: PotionEffect, color: Int = ClickGUI.primary): TextComponent.TextLine {
        val amplifier = if (effect.amplifier > 0) " ${effect.amplifier + 1}" else ""
        val duration = Potion.getPotionDurationString(effect, 1.0f)
        val name = I18n.format(effect.potion.name)
        val textLine = TextComponent.TextLine(" ")
        textLine.add(TextComponent.TextElement(name + amplifier, color))
        textLine.add(TextComponent.TextElement(" ($duration)", durationColor))
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

    private fun makeKeyPair(effects: Collection<PotionEffect>, old: Array<SortingPair>?): Array<SortingPair> {
        if (old != null && effects.size == old.size) return old
        return Array(effects.size) { SortingPair(effects.elementAt(it)) }
    }

    private enum class GradientMode {
        STATIC_GRADIENT, ANIMATED_GRADIENT
    }

    private class EffectToggleFlag(effect: PotionEffect) : TimedFlag<Boolean>(true) {
        fun update(effect: PotionEffect?) {
            value = effect != null
        }
    }

    private enum class SortingMode(override val displayName: CharSequence, val keySelector: (PotionEffect) -> Comparable<*>) : DisplayEnum {
        LENGTH("Length", {
            val name = I18n.format(it.potion.name)
            val amplifier = if (it.amplifier > 0) " ${it.amplifier + 1}" else ""
            val duration = Potion.getPotionDurationString(it, 1.0f)
            -MainFontRenderer.getWidth(name + amplifier + " ($duration)")
        }),
        DURATION("Duration", { it.duration })
    }

    private data class SortingPair(val effect: PotionEffect, var key: Comparable<*> = EffectHud.sortingMode.keySelector(effect)) : Comparable<SortingPair> {
        fun update() {
            key = EffectHud.sortingMode.keySelector(effect)
        }

        override fun compareTo(other: SortingPair): Int {
            return (key as Comparable<Any>).compareTo(other.key)
        }
    }
}
