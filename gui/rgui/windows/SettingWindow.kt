package dev.wizard.meta.gui.rgui.windows

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.wizard.meta.MetaMod
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.InteractiveComponent
import dev.wizard.meta.gui.rgui.component.*
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.groups.SettingGroup
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.setting.settings.impl.number.NumberSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.util.ClipboardUtils
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.text.NoSpamMessage
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.lwjgl.input.Keyboard
import kotlin.math.max
import kotlin.math.min

abstract class SettingWindow<T : Any>(
    screen: IGuiScreen,
    name: CharSequence,
    val element: T,
    uiSettingGroup: Component.UiSettingGroup
) : ListWindow(screen, name, uiSettingGroup) {

    private val colorPickers = Object2ObjectOpenHashMap<ColorSetting, ColorPicker>()
    private var activeColorPicker: ColorPicker? = null

    override val minWidth get() = max(super.minWidth, optimalWidth)
    override val minHeight get() = optimalHeight
    override val maxHeight get() = optimalHeight
    override val minimizable = false

    protected abstract val elementSettingGroup: SettingGroup
    protected abstract val elementSettingList: List<AbstractSetting<*>>

    private fun displayColorPicker(colorSetting: ColorSetting) {
        activeColorPicker?.let { screen.closeWindow(it) }
        val colorPicker = colorPickers.getOrPut(colorSetting) { ColorPicker(screen, this, colorSetting) }
        screen.displayWindow(colorPicker)
        activeColorPicker = colorPicker
    }

    override fun onDisplayed() {
        screen.lastClicked = this
        children.clear()
        for (setting in elementSettingList) {
            val slider = when (setting) {
                is BooleanSetting -> SettingButton(screen, setting)
                is NumberSetting -> SettingSlider(screen, setting)
                is EnumSetting -> EnumSlider(screen, setting)
                is ColorSetting -> Button(screen, setting.name, setting.description, setting.visibility).action { _, _ -> displayColorPicker(setting) }
                is StringSetting -> StringButton(screen, setting)
                is BindSetting -> BindButton(screen, setting)
                else -> null
            }
            slider?.let { children.add(it) }
        }
        super.onDisplayed()
        val mousePos = screen.mousePos
        val scaledWidth = mc.displayWidth.toFloat() / ClickGUI.scaleFactor
        val scaledHeight = mc.displayHeight.toFloat() / ClickGUI.scaleFactor
        forcePosX = if (Vec2f.getX(mousePos) + width <= scaledWidth) Vec2f.getX(mousePos) else Vec2f.getX(mousePos) - width
        forcePosY = min(Vec2f.getY(mousePos), scaledHeight - height)
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        (hoveredChild as? Slider)?.let {
            if (it != keybordListening) {
                (keybordListening as? Slider)?.onStopListening(false)
                keybordListening = if (it.listening) it else null
            }
        }
    }

    override fun onTick() {
        if (screen.lastClicked != this && (activeColorPicker == null || screen.lastClicked != activeColorPicker)) {
            screen.closeWindow(this)
            return
        }
        super.onTick()
        if (keybordListening is Slider && !(keybordListening as Slider).listening) {
            keybordListening = null
        }
        Keyboard.enableRepeatEvents(keybordListening != null)
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        screen.closeWindow(this)
    }

    override fun onClosed() {
        super.onClosed()
        keybordListening = null
        activeColorPicker?.let { screen.closeWindow(it) }
        activeColorPicker = null
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        val listening = keybordListening
        if (listening != null) {
            listening.onKeyInput(keyCode, keyState)
        } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            try {
                when (keyCode) {
                    Keyboard.KEY_C -> {
                        val jsonString = gson.toJson(elementSettingGroup.write())
                        ClipboardUtils.copyToClipboard(jsonString)
                        NoSpamMessage.sendMessage("Module config copied to clipboard")
                    }
                    Keyboard.KEY_V -> {
                        val jsonString = ClipboardUtils.pasteFromClipboard()
                        if (jsonString.isNotBlank()) {
                            val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
                            elementSettingGroup.read(jsonObject)
                            NoSpamMessage.sendMessage("Module config loaded from clipboard")
                        }
                    }
                }
            } catch (e: Exception) {
                NoSpamMessage.sendError("Failed to copy/paste settings")
                MetaMod.logger.error("Failed to copy/paste settings", e)
            }
        }
    }

    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }
}
