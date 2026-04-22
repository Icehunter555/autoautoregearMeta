package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.InteractiveComponent
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.math.vector.Vec2d
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import kotlin.math.max

open class Slider(
    screen: IGuiScreen,
    name: CharSequence,
    val description: CharSequence = "",
    val visibility: (() -> Boolean)? = null
) : InteractiveComponent(screen, name, Component.UiSettingGroup.NONE) {

    var inputField = ""
    protected val renderProgress = AnimationFlag { time, prev, current ->
        Easing.OUT_QUART.incOrDec(Easing.toDelta(time, 300.0f), prev.coerceIn(0.0f, 1.0f), current.coerceIn(0.0f, 1.0f))
    }

    override val minWidth by FrameFloat { MainFontRenderer.getWidth(name) + 20.0f + protectedWidth }
    override val maxHeight by FrameFloat { MainFontRenderer.height + 3.0f }

    var protectedWidth = 0.0f
    protected val displayDescription = TextComponent(" ")
    private var descriptionPosX = 0.0f
    private var shown = false
    var listening = false
        protected set

    override var posY: Float
        get() = if (!visible) super.posY + 100.0f else super.posY
        set(value) { super.posY = value }

    open fun onStopListening(success: Boolean) {
        listening = false
    }

    private fun setupDescription() {
        displayDescription.clear()
        if (description.isBlank()) return
        val sb = StringBuilder()
        val spaceWidth = MainFontRenderer.getWidth(" ")
        var lineWidth = -spaceWidth
        description.split(' ').forEach { word ->
            val wordWidth = MainFontRenderer.getWidth(word) + spaceWidth
            if (lineWidth + wordWidth > 169.0f) {
                displayDescription.addLine(sb.toString())
                sb.clear()
                lineWidth = -spaceWidth + wordWidth
            } else {
                lineWidth += wordWidth
            }
            sb.append(word).append(' ')
        }
        if (sb.isNotEmpty()) displayDescription.addLine(sb.toString())
    }

    override fun onTick() {
        super.onTick()
        height = maxHeight
        visibility?.let { visible = it() }
        if (!visible) renderProgress.forceUpdate(0.0f)
    }

    override fun onDisplayed() {
        height = maxHeight
        visibility?.let { visible = it() }
        super.onDisplayed()
        renderProgress.forceUpdate(0.0f)
        setupDescription()
        (maxHeight as FrameFloat).updateLazy()
        (minWidth as FrameFloat).updateLazy()
    }

    override fun onClosed() {
        super.onClosed()
        onStopListening(false)
    }

    open fun getProgress(): Float = 0.0f

    override fun onRender(absolutePos: Long) {
        val progress = renderProgress.getAndUpdate(getProgress())
        if (progress > 0.0f) {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth * progress, renderHeight, ClickGUI.primary)
        }
        val overlayColor = ColorRGB.mix(getStateColor(prevState), getStateColor(mouseState), Easing.OUT_EXPO.inc(Easing.toDelta(lastStateUpdateTime, 300.0f)))
        RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth, renderHeight, overlayColor)

        val displayText = if (listening) inputField else name
        val timeFactor = Easing.toDelta(lastStateUpdateTime, 300.0f)
        val scale = Easing.OUT_BACK.incOrDec(timeFactor, if (prevState == MouseState.NONE) 0.0f else 1.0f, if (mouseState == MouseState.NONE) 0.0f else 1.0f)
        val clickedScale = Easing.OUT_CUBIC.incOrDec(timeFactor, if (prevState == MouseState.CLICK || prevState == MouseState.DRAG) 1.0f else 0.0f, if (mouseState == MouseState.CLICK || mouseState == MouseState.DRAG) 1.0f else 0.0f)

        MainFontRenderer.drawString(
            displayText,
            2.0f + 2.0f * scale,
            1.0f - 0.025f * scale * MainFontRenderer.height + 0.05f * clickedScale * MainFontRenderer.height,
            ClickGUI.text,
            1.0f + 0.05f * scale - 0.1f * clickedScale,
            false
        )
    }

    override fun onPostRender(absolutePos: Long) {
        var deltaTime = Easing.toDelta(lastStateUpdateTime)
        if (!((mouseState == MouseState.HOVER && deltaTime > 500L) || (prevState == MouseState.HOVER && shown))) return

        if (mouseState == MouseState.HOVER) {
            if (descriptionPosX == 0.0f) descriptionPosX = Vec2f.getX(lastMousePos)
            deltaTime -= 500L
            shown = true
        } else if (deltaTime > 250.0f) {
            descriptionPosX = 0.0f
            shown = false
            return
        }

        val alpha = if (mouseState == MouseState.HOVER) Easing.OUT_CUBIC.inc(deltaTime.toFloat() / 250.0f) else Easing.OUT_CUBIC.dec(deltaTime.toFloat() / 250.0f)
        val textWidth = displayDescription.width
        val textHeight = displayDescription.height
        val relativeCorner = Vec2f(Resolution.trollWidthF, Resolution.trollHeightF) - Vec2f(absolutePos)
        val posX = descriptionPosX.coerceIn(-Vec2f.getX(absolutePos), Vec2f.getX(relativeCorner) - textWidth - 10.0f)
        val posY = (renderHeight + 4.0f).coerceIn(-Vec2f.getY(absolutePos), Vec2f.getY(relativeCorner) - textHeight - 10.0f)

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GlStateManager.pushMatrix()
        GlStateManager.translate(posX, posY, 696.0f)
        RenderUtils2D.drawRoundedRectFilled(0.0f, 0.0f, textWidth + 4.0f, textHeight + 4.0f, ClickGUI.radius, ColorRGB.alpha(ClickGUI.backGround, (ColorRGB.getA(ClickGUI.backGround) * alpha).toInt()))
        if (ClickGUI.windowOutline) {
            RenderUtils2D.drawRoundedRectOutline(0.0f, 0.0f, textWidth + 4.0f, textHeight + 4.0f, ClickGUI.radius, 1.0f, ColorRGB.alpha(ClickGUI.primary, (255 * alpha).toInt()))
        }
        displayDescription.draw(Vec2d(2.0, 2.0), alpha = alpha)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GlStateManager.popMatrix()
    }

    private fun getStateColor(state: MouseState): Int {
        return when (state) {
            MouseState.NONE -> ClickGUI.idle
            MouseState.HOVER -> ClickGUI.hover
            else -> ClickGUI.click
        }
    }
}
