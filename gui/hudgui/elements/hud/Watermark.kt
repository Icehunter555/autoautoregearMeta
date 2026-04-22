package dev.wizard.meta.gui.hudgui.elements.hud

import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.gui.hudgui.elements.text.TPS
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.module.modules.misc.PingSpoof
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.InfoCalculator
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.client.renderer.GlStateManager
import kotlin.math.max

object Watermark : HudElement("Watermark", category = Category.HUD, description = "Watermark") {

    private val page by setting(this, "Page", Page.GENERAL)
    private val mode by setting(this, "Mode", Mode.LEFT_TAG, visibility = { page == Page.GENERAL })
    var color1 by setting(this, "Primary Color", ColorRGB(255, 255, 255), visibility = { page == Page.GENERAL })
    val color2 by setting(this, "Secondary Color", ColorRGB(255, 255, 255), visibility = { page == Page.GENERAL })
    private val gradientMode by setting(this, "Gradient Mode", GradientMode.ANIMATED_GRADIENT, visibility = { page == Page.GENERAL })
    val gradientSpeed by setting(this, "Animation Speed", 1.0f, 0.1f..5.0f, 0.1f, visibility = { page == Page.GENERAL })
    private val animateText by setting(this, "Animate Text", true, visibility = { page == Page.GENERAL })
    private val typeSpeed by setting(this, "Type Speed", 2, 1..10, 1, visibility = { page == Page.GENERAL && animateText })
    private val saturation by setting(this, "Saturation", 0.8f, 0.0f..1.0f, 0.01f, visibility = { page == Page.GENERAL && gradientMode == GradientMode.RAINBOW })
    private val brightness by setting(this, "Brightness", 1.0f, 0.0f..1.0f, 0.01f, visibility = { page == Page.GENERAL && gradientMode == GradientMode.RAINBOW })

    private val separator by setting(this, "Separator", " | ", visibility = { page == Page.CONTENT })
    private val userName by setting(this, "UserName", true, visibility = { page == Page.CONTENT })
    private val ping by setting(this, "Ping", true, visibility = { page == Page.CONTENT })
    private val tps by setting(this, "Tps", true, visibility = { page == Page.CONTENT })
    private val server by setting(this, "Server Info", true, visibility = { page == Page.CONTENT })
    private val mcVersion by setting(this, "Minecraft Version", false, visibility = { page == Page.CONTENT })
    private val moduleInfo by setting(this, "Module Info", false, visibility = { page == Page.CONTENT })
    private val memoryUsage by setting(this, "Memory Info", false, visibility = { page == Page.CONTENT })
    private val target by setting(this, "Target", false, visibility = { page == Page.CONTENT })

    private const val FULL_META = "Meta 0.3B-10mq29"
    private var animationState = AnimationState.TYPING
    private var currentMeta = ""
    private var tickCounter = 0
    private var charIndex = 0
    private const val DISPLAY_TIME = 60
    private const val WAIT_TIME = 20

    override val hudWidth by FrameFloat { max(buildFullTextLine().getWidth() + 4.0f, 20.0f) }
    override val hudHeight by FrameFloat { max(MainFontRenderer.height + 2.0f, 20.0f) }

    private var cachedTextComponents: List<String> = emptyList()

    init {
        parallelListener<TickEvent.Post> {
            if (animateText) {
                updateMetaAnimation()
            } else {
                currentMeta = FULL_META
            }
            cachedTextComponents = buildTextComponents(this)
        }

        relativePosX = 2.0f
        relativePosY = 2.0f
        dockingH = HAlign.LEFT
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

        drawWatermark()
        GlStateManager.popMatrix()
    }

    private fun drawWatermark() {
        GlStateManager.pushMatrix()
        val textLine = buildFullTextLine()
        val textWidth = textLine.getWidth()
        val stringPosX = textWidth * dockingH.multiplier
        val margin = 2.0f * dockingH.offset
        val yOffset = MainFontRenderer.height + 2.0f

        GlStateManager.translate(-margin - stringPosX, 0.0f, 0.0f)
        val color = getColorAtPosition(0.0f)

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

        textLine.drawLine(1.0f, HAlign.LEFT)
        GlStateManager.popMatrix()
    }

    private fun buildFullTextLine(): TextComponent.TextLine {
        val metaWidth = MainFontRenderer.getWidth(FULL_META)
        val currentMetaWidth = MainFontRenderer.getWidth(currentMeta)
        val spacePadding = metaWidth - currentMetaWidth
        val components = cachedTextComponents
        val fullTextString = currentMeta + (if (spacePadding > 0.0f) " ".repeat((spacePadding / MainFontRenderer.getWidth(" ")).toInt() + 1) else "") + components.joinToString("")
        val totalWidth = MainFontRenderer.getWidth(fullTextString)

        val textLine = TextComponent.TextLine("")
        var currentX = 0.0f

        currentMeta.forEach { c ->
            val charWidth = MainFontRenderer.getWidth(c.toString())
            val ratio = currentX / max(totalWidth, 1.0f)
            textLine.add(TextComponent.TextElement(c.toString(), getColorAtPosition(ratio)))
            currentX += charWidth
        }

        if (spacePadding > 0.0f) {
            val spaceCount = (spacePadding / MainFontRenderer.getWidth(" ")).toInt() + 1
            currentX += spacePadding
            textLine.add(TextComponent.TextElement(" ".repeat(spaceCount), ColorRGB(0, 0, 0, 0)))
        }

        components.forEach { text ->
            if (text.isNotEmpty()) {
                text.forEach { c ->
                    val charWidth = MainFontRenderer.getWidth(c.toString())
                    val ratio = currentX / max(totalWidth, 1.0f)
                    textLine.add(TextComponent.TextElement(c.toString(), getColorAtPosition(ratio)))
                    currentX += charWidth
                }
            }
        }

        if (dockingH == HAlign.RIGHT) textLine.reverse()
        return textLine
    }

    private fun buildTextComponents(event: SafeClientEvent): List<String> {
        val serverInfo = if (mc.isSingleplayer) "Singleplayer" else mc.currentServerData?.serverIP?.takeIf { it.isNotEmpty() } ?: "Limbo"
        val parts = mutableListOf<String>()

        if (userName) parts.add(mc.session.username ?: "Random")
        if (ping) parts.add(getPingSpoof())
        if (tps) parts.add("%.2f".format(CircularArray.average(TPS.tpsBuffer)))
        if (server) parts.add(serverInfo)
        if (mcVersion) parts.add(mc.version)
        if (moduleInfo) {
            val enabledCount = ModuleManager.modules.count { it.isEnabled }
            parts.add("$enabledCount / ${ModuleManager.modules.size}")
        }
        if (memoryUsage) parts.add("${getUsedMem()} / ${Runtime.getRuntime().totalMemory() / 1048576L}MB")
        if (target) parts.add(CombatManager.target?.name ?: "No Target")

        return if (parts.isNotEmpty()) listOf(separator + parts.joinToString(separator)) else emptyList()
    }

    private fun getColorAtPosition(ratio: Float): Int {
        val timeFactor = (System.currentTimeMillis() % 10000L).toFloat() / 10000.0f * gradientSpeed
        return when (gradientMode) {
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
    }

    private fun updateMetaAnimation() {
        when (animationState) {
            AnimationState.TYPING -> {
                if (tickCounter % typeSpeed == 0) {
                    if (charIndex < 16) {
                        currentMeta = FULL_META.substring(0, charIndex + 1)
                        charIndex++
                    } else {
                        animationState = AnimationState.DISPLAYING
                        tickCounter = 0
                    }
                }
                tickCounter++
            }
            AnimationState.DISPLAYING -> {
                tickCounter++
                if (tickCounter >= DISPLAY_TIME) {
                    animationState = AnimationState.BACKSPACING
                    tickCounter = 0
                    charIndex = 15
                }
            }
            AnimationState.BACKSPACING -> {
                if (tickCounter % typeSpeed == 0) {
                    if (charIndex >= 0) {
                        currentMeta = FULL_META.substring(0, charIndex + 1)
                        charIndex--
                    } else {
                        animationState = AnimationState.WAITING
                        tickCounter = 0
                        currentMeta = ""
                    }
                }
                tickCounter++
            }
            AnimationState.WAITING -> {
                tickCounter++
                if (tickCounter >= WAIT_TIME) {
                    animationState = AnimationState.TYPING
                    tickCounter = 0
                    charIndex = 0
                    currentMeta = ""
                }
            }
        }
    }

    private fun getPingSpoof(): String {
        val minus = if (PingSpoof.isEnabled) PingSpoof.delay else 0
        val pingValue = InfoCalculator.ping() - minus
        return if (pingValue <= 0) {
            if (PingSpoof.isEnabled) "0ms [+${PingSpoof.delay}]" else "0ms"
        } else {
            if (PingSpoof.isEnabled) "${pingValue}ms [+${PingSpoof.delay}]" else "${pingValue}ms"
        }
    }

    private fun getUsedMem(): Long = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L

    private enum class Mode {
        LEFT_TAG, RIGHT_TAG, FRAME
    }

    private enum class GradientMode {
        RAINBOW, STATIC_GRADIENT, ANIMATED_GRADIENT
    }

    private enum class AnimationState {
        TYPING, DISPLAYING, BACKSPACING, WAITING
    }

    private enum class Page {
        GENERAL, CONTENT
    }
}
