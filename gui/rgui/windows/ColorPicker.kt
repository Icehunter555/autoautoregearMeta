package dev.wizard.meta.gui.rgui.windows

import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorHSB
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.color.ColorUtils
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.gui.rgui.component.*
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

class ColorPicker(screen: IGuiScreen, val parent: WindowComponent, val setting: ColorSetting) : TitledWindow(screen, "Color Picker", Component.UiSettingGroup.NONE) {

    private var fieldHeight = 0.0f
    private var fieldPos = Vec2f.ZERO to Vec2f.ZERO
    private var huePos = Vec2f.ZERO to Vec2f.ZERO
    private var hueLinePos = Vec2f.ZERO to Vec2f.ZERO
    private var prevColorPos = Vec2f.ZERO to Vec2f.ZERO
    private var currentColorPos = Vec2f.ZERO to Vec2f.ZERO

    private var hue = 0.0f
    private var saturation = 1.0f
    private var brightness = 1.0f
    private var prevHue = 0.0f
    private var prevSaturation = 1.0f
    private var prevBrightness = 1.0f

    private val r = IntegerSetting("Red", ColorRGB.getR(setting.value.unbox()), 0..255, 1)
    private val g = IntegerSetting("Green", ColorRGB.getG(setting.value.unbox()), 0..255, 1)
    private val b = IntegerSetting("Blue", ColorRGB.getB(setting.value.unbox()), 0..255, 1)
    private val a = IntegerSetting("Alpha", ColorRGB.getA(setting.value.unbox()), 0..255, 1, visibility = { setting.hasAlpha })

    private val sliderR = SettingSlider(screen, r)
    private val sliderG = SettingSlider(screen, g)
    private val sliderB = SettingSlider(screen, b)
    private val sliderA = SettingSlider(screen, a)

    private val buttonOkay = Button(screen, "Okay").action { _, _ -> actionOk() }
    private val buttonCancel = Button(screen, "Cancel").action { _, _ -> actionCancel() }
    private val buttonState = CheckButton(screen, "Sync")

    private val components = arrayOf<Slider>(sliderR, sliderG, sliderB, sliderA, buttonOkay, buttonCancel, buttonState)

    override val resizable = false
    override val minimizable = false

    private var hoveredChildSlider: Slider? = null
        set(value) {
            if (value == field) return
            field?.onLeave(screen.mousePos)
            value?.onHover(screen.mousePos)
            field = value
        }

    override fun onDisplayed() {
        val color = setting.value.unbox()
        r.value = ColorRGB.getR(color)
        g.value = ColorRGB.getG(color)
        b.value = ColorRGB.getB(color)
        a.value = ColorRGB.getA(color)
        updatePos()
        updateHSBFromRGB()
        super.onDisplayed()
        components.forEach { it.onDisplayed() }
        updatePos()
    }

    override fun onTick() {
        super.onTick()
        prevHue = hue
        prevSaturation = saturation
        prevBrightness = brightness
        components.forEach { it.onTick() }
        if (hoveredChildSlider != null) {
            updateHSBFromRGB()
        }
        if (keybordListening is Slider && !(keybordListening as Slider).listening) {
            keybordListening = null
        }
        if (buttonState.state) {
            val currentColor = ColorRGB(r.value, g.value, b.value, a.value).unbox()
            if (currentColor != ClickGUI.primary) {
                actionSync()
            }
        }
    }

    override fun onMouseInput(mousePos: Long) {
        super.onMouseInput(mousePos)
        hoveredChildSlider = components.firstOrNull { 
            it.visible && Vec2f.getX(preDragMousePos) in it.posX..(it.posX + it.width) && Vec2f.getY(preDragMousePos) in it.posY..(it.posY + it.height)
        }?.also { it.onMouseInput(mousePos) }
    }

    override fun onClick(mousePos: Long, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        val relativeMousePos = Vec2f.minus(mousePos, posX, posY)
        hoveredChildSlider?.let {
            it.onClick(Vec2f.minus(relativeMousePos, it.posX, it.posY), buttonId)
            return
        }
        updateValues(relativeMousePos, relativeMousePos)
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        val relativeMousePos = Vec2f.minus(mousePos, posX, posY)
        hoveredChildSlider?.let {
            it.onRelease(Vec2f.minus(relativeMousePos, it.posX, it.posY), clickPos, buttonId)
            if (it.listening) keybordListening = it
            return
        }
        updateValues(relativeMousePos, relativeMousePos)
    }

    override fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        val relativeMousePos = Vec2f.minus(mousePos, posX, posY)
        val relativeClickPos = Vec2f.minus(clickPos, posX, posY)
        hoveredChildSlider?.let {
            it.onDrag(Vec2f.minus(relativeMousePos, it.posX, it.posY), Vec2f.minus(relativeClickPos, it.posX, it.posY), buttonId)
            return
        }
        updateValues(relativeMousePos, relativeClickPos)
    }

    private fun updateValues(mousePos: Long, clickPos: Long) {
        val relativeX = Vec2f.getX(mousePos) - 4.0f
        val relativeY = Vec2f.getY(mousePos) - draggableHeight - 4.0f
        if (isInPair(clickPos, fieldPos)) {
            saturation = (relativeX / fieldHeight).coerceIn(0.0f, 1.0f)
            brightness = (1.0f - relativeY / fieldHeight).coerceIn(0.0f, 1.0f)
            updateRGBFromHSB()
        } else if (isInPair(clickPos, huePos)) {
            hue = (relativeY / fieldHeight).coerceIn(0.0f, 1.0f)
            updateRGBFromHSB()
        }
    }

    private fun isInPair(mousePos: Long, pair: Pair<Vec2f, Vec2f>): Boolean {
        val x = Vec2f.getX(mousePos)
        val y = Vec2f.getY(mousePos)
        return x in pair.first.x..pair.second.x && y in pair.first.y..pair.second.y
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        keybordListening?.onKeyInput(keyCode, keyState)
    }

    override fun onRender(absolutePos: Long) {
        super.onRender(absolutePos)
        GlStateUtils.texture2d(false)
        GlStateUtils.smooth(true)
        drawColorField()
        drawHueSlider()
        drawColorPreview()
        GlStateUtils.smooth(false)
        GlStateUtils.texture2d(true)
        components.forEach {
            if (it.visible) {
                GlStateManager.pushMatrix()
                GlStateManager.translate(it.renderPosX, it.renderPosY, 0.0f)
                it.onRender(Vec2f.plus(absolutePos, it.renderPosX, it.renderPosY))
                GlStateManager.popMatrix()
            }
        }
    }

    override fun onPostRender(absolutePos: Long) {
        super.onPostRender(absolutePos)
        components.forEach {
            if (it.visible) {
                GlStateManager.pushMatrix()
                GlStateManager.translate(it.renderPosX, it.renderPosY, 0.0f)
                it.onPostRender(Vec2f.plus(absolutePos, it.renderPosX, it.renderPosY))
                GlStateManager.popMatrix()
            }
        }
    }

    private fun drawColorField() {
        RenderUtils2D.prepareGL()
        val interpolatedHue = prevHue + (hue - prevHue) * mc.renderPartialTicks
        val rightColor = ColorUtils.hsbToRGB(interpolatedHue, 1.0f, 1.0f, 1.0f)
        val leftColor = ColorRGB(255, 255, 255).unbox()
        RenderUtils2D.putVertex(fieldPos.first, leftColor)
        RenderUtils2D.putVertex(Vec2f(fieldPos.first.x, fieldPos.second.y), leftColor)
        RenderUtils2D.putVertex(Vec2f(fieldPos.second.x, fieldPos.first.y), rightColor)
        RenderUtils2D.putVertex(fieldPos.second, rightColor)
        RenderUtils2D.draw(GL11.GL_TRIANGLE_STRIP)

        val topColor = ColorRGB(0, 0, 0, 0).unbox()
        val bottomColor = ColorRGB(0, 0, 0, 255).unbox()
        RenderUtils2D.putVertex(fieldPos.first, topColor)
        RenderUtils2D.putVertex(Vec2f(fieldPos.first.x, fieldPos.second.y), bottomColor)
        RenderUtils2D.putVertex(Vec2f(fieldPos.second.x, fieldPos.first.y), topColor)
        RenderUtils2D.putVertex(fieldPos.second, bottomColor)
        RenderUtils2D.draw(GL11.GL_TRIANGLE_STRIP)
        RenderUtils2D.releaseGL()

        val interpolatedSaturation = MathUtils.lerp(prevSaturation, saturation, RenderUtils3D.getPartialTicks())
        val interpolatedBrightness = MathUtils.lerp(prevBrightness, brightness, RenderUtils3D.getPartialTicks())
        val relativeBrightness = ((1.0f - (1.0f - interpolatedSaturation) * interpolatedBrightness) * 255.0f).toInt()
        val circleColor = ColorRGB(relativeBrightness, relativeBrightness, relativeBrightness).unbox()
        val circlePos = Vec2f(fieldPos.first.x + fieldHeight * interpolatedSaturation, fieldPos.first.y + fieldHeight * (1.0f - interpolatedBrightness))
        RenderUtils2D.drawCircleOutline(circlePos.unbox(), 4.0f, 32, 1.5f, circleColor)
    }

    private fun drawHueSlider() {
        val colors = intArrayOf(
            ColorRGB(255, 0, 0).unbox(),
            ColorRGB(255, 255, 0).unbox(),
            ColorRGB(0, 255, 0).unbox(),
            ColorRGB(0, 255, 255).unbox(),
            ColorRGB(0, 0, 255).unbox(),
            ColorRGB(255, 0, 255).unbox(),
            ColorRGB(255, 0, 0).unbox()
        )
        val partHeight = (huePos.second.y - huePos.first.y) / 6.0f
        RenderUtils2D.prepareGL()
        for (i in 0 until 6) {
            RenderUtils2D.putVertex(Vec2f.plus(huePos.first.unbox(), 0.0f, partHeight * i), colors[i])
            RenderUtils2D.putVertex(Vec2f.plus(huePos.first.unbox(), 8.0f, partHeight * i), colors[i])
            RenderUtils2D.putVertex(Vec2f.plus(huePos.first.unbox(), 0.0f, partHeight * (i + 1)), colors[i + 1])
            RenderUtils2D.putVertex(Vec2f.plus(huePos.first.unbox(), 8.0f, partHeight * (i + 1)), colors[i + 1])
        }
        RenderUtils2D.draw(GL11.GL_TRIANGLE_STRIP)
        RenderUtils2D.releaseGL()

        val interpolatedHue = prevHue + (hue - prevHue) * mc.renderPartialTicks
        val pointerPosY = huePos.first.y + fieldHeight * interpolatedHue
        RenderUtils2D.drawTriangleOutline(Vec2f(huePos.first.x - 5.0f, pointerPosY - 2.0f), Vec2f(huePos.first.x - 5.0f, pointerPosY + 2.0f), Vec2f(huePos.first.x - 1.0f, pointerPosY), 1.5f, ClickGUI.primary)
        RenderUtils2D.drawTriangleOutline(Vec2f(huePos.second.x + 1.0f, pointerPosY), Vec2f(huePos.second.x + 5.0f, pointerPosY + 2.0f), Vec2f(huePos.second.x + 5.0f, pointerPosY - 2.0f), 1.5f, ClickGUI.primary)
    }

    private fun drawColorPreview() {
        RenderUtils2D.prepareGL()
        val prevColor = ColorRGB.alpha(setting.value.unbox(), 255)
        RenderUtils2D.drawRectFilled(prevColorPos.first.x, prevColorPos.first.y, prevColorPos.second.x, prevColorPos.second.y, prevColor)
        val currentColor = ColorRGB(r.value, g.value, b.value).unbox()
        RenderUtils2D.drawRectFilled(currentColorPos.first.x, currentColorPos.first.y, currentColorPos.second.x, currentColorPos.second.y, currentColor)
        RenderUtils2D.releaseGL()
    }

    private fun actionOk() {
        setting.value = ColorRGB(r.value, g.value, b.value, a.value)
        screen.closeWindow(this)
        screen.lastClicked = parent
    }

    private fun actionSync() {
        val primary = ClickGUI.primary
        r.value = ColorRGB.getR(primary)
        g.value = ColorRGB.getG(primary)
        b.value = ColorRGB.getB(primary)
        a.value = ColorRGB.getA(primary)
        setting.value = ColorRGB(r.value, g.value, b.value, a.value)
        updateHSBFromRGB()
        screen.lastClicked = parent
    }

    private fun actionCancel() {
        screen.closeWindow(this)
        screen.lastClicked = parent
    }

    private fun updateRGBFromHSB() {
        val color = ColorUtils.hsbToRGB(hue, saturation, brightness)
        r.value = ColorRGB.getR(color)
        g.value = ColorRGB.getG(color)
        b.value = ColorRGB.getB(color)
    }

    private fun updateHSBFromRGB() {
        val hsb = ColorUtils.rgbToHSB(r.value, g.value, b.value)
        hue = hsb.h
        saturation = hsb.s
        brightness = hsb.b
    }

    private fun updatePos() {
        sliderR.forcePosY = draggableHeight
        sliderR.forceWidth = 128.0f
        sliderG.forcePosY = sliderR.forcePosY + sliderR.forceHeight + 2.0f
        sliderG.forceWidth = 128.0f
        sliderB.forcePosY = sliderG.forcePosY + sliderG.forceHeight + 2.0f
        sliderB.forceWidth = 128.0f
        sliderA.forcePosY = sliderB.forcePosY + sliderB.forceHeight + 2.0f
        sliderA.forceWidth = 128.0f
        buttonOkay.forcePosY = sliderA.forcePosY + sliderA.forceHeight + 2.0f
        buttonOkay.forceWidth = 50.0f
        buttonCancel.forcePosY = buttonOkay.forcePosY + buttonOkay.forceHeight + 2.0f
        buttonCancel.forceWidth = 50.0f
        buttonState.forcePosY = buttonCancel.forcePosY + buttonCancel.forceHeight + 2.0f
        buttonState.forceWidth = 50.0f

        dockingH = HAlign.CENTER
        dockingV = VAlign.CENTER
        relativePosXSetting.value = 0.0f
        relativePosYSetting.value = 0.0f

        forceHeight = buttonState.forcePosY + buttonState.forceHeight + 4.0f
        forceWidth = forceHeight - draggableHeight + 4.0f + 8.0f + 4.0f + 128.0f + 8.0f

        sliderR.forcePosX = forceWidth - 4.0f - 128.0f
        sliderG.forcePosX = forceWidth - 4.0f - 128.0f
        sliderB.forcePosX = forceWidth - 4.0f - 128.0f
        sliderA.forcePosX = forceWidth - 4.0f - 128.0f
        buttonOkay.forcePosX = forceWidth - 4.0f - 50.0f
        buttonCancel.forcePosX = buttonOkay.forcePosX
        buttonState.forcePosX = buttonOkay.forcePosX

        fieldHeight = forceHeight - draggableHeight - 4.0f
        fieldPos = Vec2f(4.0f, draggableHeight) to Vec2f(4.0f + fieldHeight, draggableHeight + fieldHeight)
        huePos = Vec2f(4.0f + fieldHeight + 6.0f, draggableHeight) to Vec2f(4.0f + fieldHeight + 6.0f + 8.0f, draggableHeight + fieldHeight)
        prevColorPos = Vec2f(sliderR.forcePosX, buttonOkay.forcePosY) to Vec2f(sliderR.forcePosX + 35.0f, forceHeight - 4.0f)
        currentColorPos = Vec2f(sliderR.forcePosX + 35.0f + 4.0f, buttonOkay.forcePosY) to Vec2f(sliderR.forcePosX + 35.0f + 4.0f + 35.0f, forceHeight - 4.0f)
    }
}
