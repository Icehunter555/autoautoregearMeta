package dev.wizard.meta.gui.rgui.windows

import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.threads.runSynchronized

class DockingOverlay(screen: IGuiScreen, private val parent: WindowComponent) : WindowComponent(screen, "Docking Overlay", Component.UiSettingGroup.NONE, GuiConfig) {

    private val alphaMul = AnimationFlag(Easing.OUT_QUART, 300.0f)
    val renderAlphaMul by FrameFloat(alphaMul::get)
    private var closing = false

    override var posX: Float
        get() = 0.0f
        set(value) {}
    override var posY: Float
        get() = 0.0f
        set(value) {}
    override var width: Float
        get() = Resolution.trollWidthF
        set(value) {}
    override var height: Float
        get() = Resolution.trollHeightF
        set(value) {}

    override fun onGuiClosed() {
        super.onGuiClosed()
        screen.closeWindow(this)
    }

    override fun onDisplayed() {
        super.onDisplayed()
        closing = false
        alphaMul.forceUpdate(0.0f, 1.0f)
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        closing = true
        alphaMul.update(0.0f)
        val fifthWidth = width / 5.0f
        val fifthHeight = height / 5.0f
        val dockingH = getDocking(HAlign.entries, Vec2f.getX(screen.mousePos), fifthWidth)
        val dockingV = getDocking(VAlign.entries, Vec2f.getY(screen.mousePos), fifthHeight)
        if (dockingH != null && dockingV != null) {
            parent.dockingH = dockingH
            parent.dockingV = dockingV
        }
    }

    override fun onTick() {
        super.onTick()
        if (renderAlphaMul == 0.0f && closing) {
            screen.closeWindow(this)
        }
    }

    override fun onPostRender(absolutePos: Long) {
        val fifthWidth = width / 5.0f
        val fifthHeight = height / 5.0f
        val rectColor = ColorRGB.alpha(ClickGUI.backGround, (ColorRGB.getA(ClickGUI.backGround) * renderAlphaMul).toInt())
        
        drawRect(fifthWidth, fifthHeight, rectColor, 0, 0)
        drawRect(fifthWidth, fifthHeight, rectColor, 2, 0)
        drawRect(fifthWidth, fifthHeight, rectColor, 4, 0)
        drawRect(fifthWidth, fifthHeight, rectColor, 0, 2)
        drawRect(fifthWidth, fifthHeight, rectColor, 2, 2)
        drawRect(fifthWidth, fifthHeight, rectColor, 4, 2)
        drawRect(fifthWidth, fifthHeight, rectColor, 0, 4)
        drawRect(fifthWidth, fifthHeight, rectColor, 2, 4)
        drawRect(fifthWidth, fifthHeight, rectColor, 4, 4)

        val textColor = ColorRGB.alpha(ClickGUI.text, (ColorRGB.getA(ClickGUI.text) * renderAlphaMul).toInt())
        drawText(fifthWidth, fifthHeight, textColor, 0, 0, "Top Left")
        drawText(fifthWidth, fifthHeight, textColor, 2, 0, "Top Center")
        drawText(fifthWidth, fifthHeight, textColor, 4, 0, "Top Right")
        drawText(fifthWidth, fifthHeight, textColor, 0, 2, "Middle Left")
        drawText(fifthWidth, fifthHeight, textColor, 2, 2, "Middle Center")
        drawText(fifthWidth, fifthHeight, textColor, 4, 2, "Middle Right")
        drawText(fifthWidth, fifthHeight, textColor, 0, 4, "Bottom Left")
        drawText(fifthWidth, fifthHeight, textColor, 2, 4, "Bottom Center")
        drawText(fifthWidth, fifthHeight, textColor, 4, 4, "Bottom Right")
    }

    private fun <T> getDocking(entries: List<T>, v: Float, step: Float): T? {
        return when {
            v in 0.0f..step -> entries[0]
            v in (step * 2.0f)..(step * 3.0f) -> entries[1]
            v in (step * 4.0f)..(step * 5.0f) -> entries[2]
            else -> null
        }
    }

    private fun drawRect(fifthWidth: Float, fifthHeight: Float, rectColor: Int, x: Int, y: Int) {
        RenderUtils2D.drawRectFilled(fifthWidth * x, fifthHeight * y, fifthWidth * (x + 1), fifthHeight * (y + 1), rectColor)
    }

    private fun drawText(fifthWidth: Float, fifthHeight: Float, textColor: Int, x: Int, y: Int, text: String) {
        val scale = 1.5f
        MainFontRenderer.drawString(text, fifthWidth * (x + 0.5f) - MainFontRenderer.getWidth(text, scale) / 2.0f, fifthHeight * (y + 0.5f) - MainFontRenderer.height * scale / 2.0f, textColor, scale, false)
    }
}
