package dev.wizard.meta.gui.rgui

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.util.interfaces.Nameable
import dev.wizard.meta.util.math.vector.Vec2f

open class InteractiveComponent(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: Component.UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : Component(screen, name, uiSettingGroup, config) {

    protected var lastMousePos = Vec2f.ZERO
    protected var lastClickPos = Vec2f.ZERO
    var mouseState = MouseState.NONE
        private set(value) {
            prevState = field
            lastStateUpdateTime = System.currentTimeMillis()
            field = value
        }

    protected var prevState = MouseState.NONE
        private set
    protected var lastStateUpdateTime = System.currentTimeMillis()
        private set

    override fun onDisplayed() {
        super.onDisplayed()
        mouseState = MouseState.NONE
        prevState = MouseState.NONE
        lastStateUpdateTime = System.currentTimeMillis()
    }

    open fun onMouseInput(mousePos: Long) {
        lastMousePos = mousePos
    }

    open fun onHover(mousePos: Long) {
        mouseState = MouseState.HOVER
    }

    open fun onLeave(mousePos: Long) {
        mouseState = MouseState.NONE
    }

    open fun onClick(mousePos: Long, buttonId: Int) {
        mouseState = MouseState.CLICK
    }

    open fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        mouseState = MouseState.HOVER
    }

    open fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        mouseState = MouseState.DRAG
        lastClickPos = clickPos
    }

    open fun onKeyInput(keyCode: Int, keyState: Boolean) {}
}
