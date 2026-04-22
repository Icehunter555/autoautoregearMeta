package dev.wizard.meta.gui.rgui

import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.AnimationFlag
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.HAlign
import dev.wizard.meta.graphics.VAlign
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.extension.getRootName
import dev.wizard.meta.util.interfaces.Nameable
import net.minecraft.client.Minecraft
import kotlin.math.max

open class Component(
    open val screen: IGuiScreen,
    override val name: CharSequence,
    val uiSettingGroup: UiSettingGroup,
    val config: AbstractConfig<out Nameable> = GuiConfig
) : Nameable {

    override val internalName = name.toString().getRootName().replace(" ", "")

    val visibleSetting = config.setting(this, "Visible", true, { false }, { _, it -> it || !closeable })
    var visible by visibleSetting

    val dockingHSetting = config.setting(this, "Docking H", HAlign.LEFT)
    var dockingH by dockingHSetting

    val dockingVSetting = config.setting(this, "Docking V", VAlign.TOP)
    var dockingV by dockingVSetting

    val widthSetting = config.setting(this, "Width", 1.0f, 0.0f..69420.914f, 0.1f, { false }, { _, it -> it.coerceIn(minWidth, max(scaledDisplayWidth, minWidth)) })
    val heightSetting = config.setting(this, "Height", 1.0f, 0.0f..69420.914f, 0.1f, { false }, { _, it -> it.coerceIn(minHeight, max(scaledDisplayHeight, minHeight)) })
    val relativePosXSetting = config.setting(this, "Pos X", 0.0f, -69420.914f..69420.914f, 0.1f, { false }, { _, it -> if (this is WindowComponent && MetaMod.isReady) a2rX(it.coerceIn(0.0f, max(scaledDisplayWidth - widthSetting.value, 0.0f))) else it })
    val relativePosYSetting = config.setting(this, "Pos Y", 0.0f, -69420.914f..69420.914f, 0.1f, { false }, { _, it -> if (this is WindowComponent && MetaMod.isReady) a2rY(it.coerceIn(0.0f, max(scaledDisplayHeight - heightSetting.value, 0.0f))) else it })

    protected val mc: Minecraft = Wrapper.minecraft

    open val minWidth = 1.0f
    open val minHeight = 1.0f
    open val maxWidth = -1.0f
    open val maxHeight = -1.0f

    private val renderPosXFlag = AnimationFlag { time, prev, current -> r2aX(Easing.OUT_CUBIC.incOrDec(Easing.toDelta(time, 200.0f), prev, current)) }
    private val renderPosYFlag = AnimationFlag { time, prev, current -> r2aY(Easing.OUT_CUBIC.incOrDec(Easing.toDelta(time, 200.0f), prev, current)) }
    private val renderWidthFlag = AnimationFlag(Easing.OUT_CUBIC, 300.0f)
    private val renderHeightFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    val renderPosX by FrameFloat(renderPosXFlag::get)
    val renderPosY by FrameFloat(renderPosYFlag::get)
    val renderWidth by FrameFloat(renderWidthFlag::get)
    val renderHeight by FrameFloat(renderHeightFlag::get)

    init {
        widthSetting.valueListeners.add { _, it -> renderWidthFlag.update(it) }
        heightSetting.valueListeners.add { _, it -> renderHeightFlag.update(it) }
        relativePosXSetting.valueListeners.add { _, it -> renderPosXFlag.update(it) }
        relativePosYSetting.valueListeners.add { _, it -> renderPosYFlag.update(it) }

        dockingHSetting.valueListeners.add { prev, current ->
            relativePosXSetting.value = a2rX(r2aX(relativePosXSetting.value, prev), current)
            renderPosXFlag.forceUpdate(relativePosXSetting.value)
        }
        dockingVSetting.valueListeners.add { prev, current ->
            relativePosYSetting.value = a2rY(r2aY(relativePosYSetting.value, prev), current)
            renderPosYFlag.forceUpdate(relativePosYSetting.value)
        }
    }

    val settingGroup get() = config.getGroupOrPut(uiSettingGroup.groupName).getGroupOrPut(internalName)

    var posX: Float
        get() = r2aX(relativePosXSetting.value)
        set(value) {
            if (!MetaMod.isReady) return
            relativePosXSetting.value = a2rX(value)
        }

    var forcePosX: Float
        get() = posX
        set(value) {
            posX = value
            renderPosXFlag.forceUpdate(relativePosXSetting.value)
        }

    var posY: Float
        get() = r2aY(relativePosYSetting.value)
        set(value) {
            if (!MetaMod.isReady) return
            relativePosYSetting.value = a2rY(value)
        }

    var forcePosY: Float
        get() = posY
        set(value) {
            posY = value
            renderPosYFlag.forceUpdate(relativePosYSetting.value)
        }

    var width: Float
        get() {
            var value = widthSetting.value
            if (maxWidth != -1.0f && value > maxWidth) value = maxWidth
            if (value < minWidth) value = minWidth
            widthSetting.value = value
            return value
        }
        set(value) {
            widthSetting.value = value
        }

    var forceWidth: Float
        get() = width
        set(value) {
            width = value
            renderWidthFlag.forceUpdate(widthSetting.value)
        }

    var height: Float
        get() {
            var value = heightSetting.value
            if (maxHeight != -1.0f && value > maxHeight) value = maxHeight
            if (value < minHeight) value = minHeight
            heightSetting.value = value
            return value
        }
        set(value) {
            heightSetting.value = value
        }

    var forceHeight: Float
        get() = height
        set(value) {
            height = value
            renderHeightFlag.forceUpdate(heightSetting.value)
        }

    open val closeable = true

    fun r2aX(x: Float, docking: HAlign = dockingH): Float = x + scaledDisplayWidth * docking.multiplier - dockWidth(docking)
    fun r2aY(y: Float, docking: VAlign = dockingV): Float = y + scaledDisplayHeight * docking.multiplier - dockHeight(docking)
    fun a2rX(x: Float, docking: HAlign = dockingH): Float = x - scaledDisplayWidth * docking.multiplier + dockWidth(docking)
    fun a2rY(y: Float, docking: VAlign = dockingV): Float = y - scaledDisplayHeight * docking.multiplier + dockHeight(docking)

    private fun dockWidth(docking: HAlign): Float = width * docking.multiplier
    private fun dockHeight(docking: VAlign): Float = height * docking.multiplier

    protected val scaledDisplayWidth get() = mc.displayWidth.toFloat() / ClickGUI.scaleFactor
    protected val scaledDisplayHeight get() = mc.displayHeight.toFloat() / ClickGUI.scaleFactor

    open fun onGuiDisplayed() = onDisplayed()
    open fun onGuiClosed() = onClosed()

    open fun onDisplayed() {
        renderPosXFlag.forceUpdate(relativePosXSetting.value)
        renderPosYFlag.forceUpdate(relativePosYSetting.value)
        renderWidthFlag.forceUpdate(widthSetting.value)
        renderHeightFlag.forceUpdate(heightSetting.value)
    }

    open fun onClosed() {}

    open fun onTick() {
        renderPosXFlag.update(relativePosXSetting.value)
        renderPosYFlag.update(relativePosYSetting.value)
        renderWidthFlag.update(widthSetting.value)
        renderHeightFlag.update(heightSetting.value)
    }

    open fun onRender(absolutePos: Long) {}
    open fun onPostRender(absolutePos: Long) {}

    enum class UiSettingGroup(val groupName: String) {
        NONE(""),
        CLICK_GUI("click_gui"),
        HUD_GUI("hud_gui")
    }
}
