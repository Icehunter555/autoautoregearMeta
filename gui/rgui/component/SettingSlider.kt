package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.number.NumberSetting
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.math.vector.Vec2f
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToLong

class SettingSlider(screen: IGuiScreen, val setting: NumberSetting<*>) : Slider(screen, setting.name, setting.description, setting.visibility) {

    private val range = setting.range.endInclusive.toDouble() - setting.range.start.toDouble()
    private val settingStep = if (setting.step.toDouble() > 0.0) setting.step else getDefaultStep()
    private val stepDouble = settingStep.toDouble()
    private val fineStepDouble = setting.fineStep.toDouble()
    private val places = when (setting) {
        is IntegerSetting -> 1
        is FloatSetting -> MathUtils.decimalPlaces(settingStep.toFloat())
        else -> MathUtils.decimalPlaces(settingStep.toDouble())
    }
    private var preDragMousePos = Vec2f.ZERO

    override fun getProgress(): Float {
        if (!setting.isVisible) return 0.0f
        if (mouseState != MouseState.DRAG && !listening) {
            val min = setting.range.start.toDouble()
            var flooredValue = floor((renderProgress.current.toDouble() * range + min) / stepDouble) * stepDouble
            if (abs(flooredValue) < 1e-9) flooredValue = 0.0
            if (abs(flooredValue - setting.value.toDouble()) >= stepDouble) {
                return ((setting.value.toDouble() - min) / range).toFloat()
            }
        }
        return Float.NaN
    }

    private fun getDefaultStep(): Number {
        return when (setting) {
            is IntegerSetting -> range / 20.0
            is FloatSetting -> range / 20.0f
            else -> range / 20.0
        }
    }

    override fun onDisplayed() {
        protectedWidth = MainFontRenderer.getWidth(setting.toString(), 0.75f)
        super.onDisplayed()
    }

    override fun onStopListening(success: Boolean) {
        if (success) {
            inputField.toDoubleOrNull()?.let {
                setting.setValue(it.toString())
            }
        }
        super.onStopListening(success)
        inputField = ""
    }

    override fun onClick(mousePos: Long, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        if (buttonId == 0) {
            preDragMousePos = mousePos
            updateValue(mousePos)
        }
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (buttonId == 1) {
            if (!listening) {
                listening = true
                inputField = setting.value.toString()
            } else {
                onStopListening(false)
            }
        } else if (buttonId == 0 && listening) {
            onStopListening(true)
        }
    }

    override fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        if (!listening && buttonId == 0) {
            updateValue(mousePos)
        }
    }

    private fun updateValue(mousePos: Long) {
        val shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
        val value = if (!shiftDown) {
            Vec2f.getX(mousePos) / width
        } else {
            (Vec2f.getX(preDragMousePos) + (Vec2f.getX(mousePos) - Vec2f.getX(preDragMousePos)) * 0.1f) / width
        }
        val step = if (shiftDown) fineStepDouble else stepDouble
        val min = setting.range.start.toDouble()
        var roundedValue = MathUtils.round((((value.toDouble() * range + min) / step).roundToLong() * step), places)
        if (abs(roundedValue) < 1e-9) roundedValue = 0.0
        setting.setValue(roundedValue)
        renderProgress.update(value)
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        val typedChar = Keyboard.getEventCharacter()
        if (keyState) {
            when (keyCode) {
                Keyboard.KEY_RETURN -> onStopListening(true)
                Keyboard.KEY_BACK, Keyboard.KEY_DELETE -> {
                    inputField = inputField.substring(0, max(inputField.length - 1, 0))
                    if (inputField.isBlank()) inputField = "0"
                }
                else -> {
                    if (isNumber(typedChar)) {
                        if (inputField == "0" && (typedChar.isDigit() || typedChar == '-')) {
                            inputField = ""
                        }
                        inputField += typedChar
                    }
                }
            }
        }
    }

    private fun isNumber(c: Char): Boolean {
        return c.isDigit() || c == '-' || c == '.' || c.equals('e', true)
    }

    override fun onRender(absolutePos: Long) {
        val valueText = setting.toString()
        protectedWidth = MainFontRenderer.getWidth(valueText, 0.75f)
        super.onRender(absolutePos)
        if (!listening) {
            val posX = renderWidth - protectedWidth - 2.0f
            val posY = renderHeight - 2.0f - MainFontRenderer.getHeight(0.75f)
            MainFontRenderer.drawString(valueText, posX, posY, ClickGUI.text, 0.75f, false)
        }
    }
}
