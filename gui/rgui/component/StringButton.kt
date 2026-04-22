package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import org.lwjgl.input.Keyboard
import kotlin.math.max

class StringButton(screen: IGuiScreen, val setting: StringSetting) : BooleanSlider(screen, setting.name, setting.description, setting.visibility) {

    override fun getProgress(): Float = if (!listening) 1.0f else 0.0f

    override fun onStopListening(success: Boolean) {
        if (success) {
            setting.value = inputField
        }
        super.onStopListening(success)
        inputField = ""
    }

    override fun onMouseInput(mousePos: Long) {
        super.onMouseInput(mousePos)
        if (!listening) {
            inputField = if (mouseState == MouseState.NONE) "" else setting.value
        }
    }

    override fun onTick() {
        super.onTick()
        if (!listening) {
            inputField = if (mouseState != MouseState.NONE) setting.value else ""
        }
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        when (buttonId) {
            0, 1 -> {
                if (listening) {
                    onStopListening(buttonId == 0)
                } else {
                    startListening()
                }
            }
        }
    }

    private fun startListening() {
        listening = true
        inputField = setting.value
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        val typedChar = Keyboard.getEventCharacter()
        if (keyState) {
            when (keyCode) {
                Keyboard.KEY_RETURN -> onStopListening(true)
                Keyboard.KEY_BACK, Keyboard.KEY_DELETE -> {
                    inputField = inputField.substring(0, max(inputField.length - 1, 0))
                }
                else -> {
                    if (typedChar.toInt() >= 32) {
                        inputField += typedChar
                    }
                }
            }
        }
    }
}
