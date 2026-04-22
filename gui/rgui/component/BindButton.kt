package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.util.Bind
import org.lwjgl.input.Keyboard

class BindButton(screen: IGuiScreen, val setting: BindSetting) : Slider(screen, setting.name, setting.description, setting.visibility) {

    override fun onDisplayed() {
        protectedWidth = MainFontRenderer.getWidth(setting.value.toString(), 0.75f)
        super.onDisplayed()
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (listening) {
            if (buttonId > 1) {
                setting.value.setBind(-buttonId - 1)
            }
        }
        listening = !listening
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        if (listening && keyCode != 0 && !keyState) {
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_BACK, Keyboard.KEY_DELETE -> setting.value.clear()
                else -> setting.value.setBind(keyCode)
            }
            inputField = setting.nameAsString
            listening = false
        }
    }

    override fun onRender(absolutePos: Long) {
        super.onRender(absolutePos)
        val valueText = if (listening) "Listening" else setting.value.toString()
        protectedWidth = MainFontRenderer.getWidth(valueText, 0.75f)
        val posX = renderWidth - protectedWidth - 2.0f
        val posY = renderHeight - 2.0f - MainFontRenderer.getHeight(0.75f)
        MainFontRenderer.drawString(valueText, posX, posY, ClickGUI.text, 0.75f, false)
    }
}
