package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.util.math.vector.Vec2f

open class Button(
    screen: IGuiScreen,
    name: CharSequence,
    description: CharSequence = "",
    visibility: (() -> Boolean)? = null
) : BooleanSlider(screen, name, description, visibility) {

    private val actions = mutableListOf<Action>()
    private var state = false

    fun action(action: Action): Button {
        actions.add(action)
        return this
    }

    override fun getProgress(): Float = if (state) 1.0f else 0.0f

    override fun onClick(mousePos: Long, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        state = true
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (state && prevState != MouseState.DRAG) {
            actions.forEach { it.invoke(mousePos, buttonId) }
        }
        state = false
    }

    override fun onLeave(mousePos: Long) {
        super.onLeave(mousePos)
        state = false
    }

    fun interface Action {
        fun invoke(mousePos: Long, buttonId: Int)
    }
}
