package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.util.math.vector.Vec2f
import kotlin.math.abs

open class CheckButton(
    screen: IGuiScreen,
    name: CharSequence,
    description: CharSequence = "",
    visibility: (() -> Boolean)? = null
) : BooleanSlider(screen, name, description, visibility) {

    open var state = false

    override fun getProgress(): Float {
        if (!visible) return 0.0f
        return if (mouseState != MouseState.DRAG) if (state) 1.0f else 0.0f else Float.NaN
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (prevState == MouseState.DRAG && abs(Vec2f.getX(mousePos) - Vec2f.getX(lastClickPos)) > 16.0f) {
            state = renderProgress.current > if (state) 0.7f else 0.3f
        } else {
            if (Vec2f.getX(mousePos) in -2.0f..renderWidth + 2.0f && Vec2f.getY(mousePos) in -2.0f..renderHeight + 2.0f) {
                state = !state
            }
        }
    }

    override fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        val prevProgress = if (state) 1.0f else 0.0f
        renderProgress.update((prevProgress + (Vec2f.getX(mousePos) - Vec2f.getX(clickPos)) / width).coerceIn(0.0f, 1.0f))
    }
}
