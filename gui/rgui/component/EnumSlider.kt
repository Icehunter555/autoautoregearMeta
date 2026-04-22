package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.extension.readableName
import dev.wizard.meta.util.math.vector.Vec2f
import kotlin.math.floor

class EnumSlider(screen: IGuiScreen, val setting: EnumSetting<*>) : Slider(screen, setting.name, setting.description, setting.visibility) {

    private val enumValues = setting.enumValues
    private var progress = 0.0f

    override fun getProgress(): Float {
        if (mouseState == MouseState.DRAG) return progress
        val settingValue = (setting.value as Enum<*>).ordinal
        val currentRound = roundInput(renderProgress.current)
        return if (currentRound != settingValue) {
            progress = (settingValue.toFloat() + settingValue.toFloat() / (enumValues.size - 1).toFloat()) / enumValues.size.toFloat()
            progress
        } else {
            Float.NaN
        }
    }

    override fun onDisplayed() {
        protectedWidth = MainFontRenderer.getWidth((setting.value as Enum<*>).readableName(), 0.75f)
        super.onDisplayed()
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (prevState != MouseState.DRAG) {
            setting.nextValue()
        }
    }

    override fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        updateValue(mousePos)
    }

    private fun updateValue(mousePos: Long) {
        progress = (Vec2f.getX(mousePos) / width).coerceIn(0.0f, 1.0f)
        setting.setValue(enumValues[roundInput(progress)].name)
    }

    private fun roundInput(input: Float): Int {
        return floor(input * enumValues.size).toInt().coerceIn(0, enumValues.size - 1)
    }

    override fun onRender(absolutePos: Long) {
        val valueText = (setting.value as Enum<*>).readableName()
        protectedWidth = MainFontRenderer.getWidth(valueText, 0.75f)
        super.onRender(absolutePos)
        val posX = renderWidth - protectedWidth - 2.0f
        val posY = renderHeight - 2.0f - MainFontRenderer.getHeight(0.75f)
        MainFontRenderer.drawString(valueText, posX, posY, ClickGUI.text, 0.75f, false)
    }
}
