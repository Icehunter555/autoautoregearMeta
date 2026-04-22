package dev.wizard.meta.gui.rgui

import dev.wizard.meta.graphics.AnimationFlag
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.graphics.HAlign
import dev.wizard.meta.graphics.VAlign
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.windows.DockingOverlay
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.interfaces.Nameable
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.threads.runSynchronized
import kotlin.math.max
import kotlin.math.min

open class WindowComponent(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: Component.UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : InteractiveComponent(screen, name, uiSettingGroup, config) {

    val minimizedSetting = config.setting(this, "Minimized", false, { false }, { _, input ->
        System.currentTimeMillis() - renderMinimizeProgressFlag.time > 300L && input
    })
    var minimized by minimizedSetting

    open var keybordListening: InteractiveComponent? = null
    var lastActiveTime = System.currentTimeMillis()
        protected set

    var preDragMousePos = Vec2f.ZERO
        private set
    var preDragPos = Vec2f.ZERO
        private set
    var preDragSize = Vec2f.ZERO
        private set

    private val renderMinimizeProgressFlag = AnimationFlag(Easing.OUT_QUART, 300.0f)
    val renderMinimizeProgress by FrameFloat(renderMinimizeProgressFlag::get)

    private val dockingOverlay by lazy { DockingOverlay(screen, this) }

    init {
        minimizedSetting.valueListeners.add { _, it ->
            renderMinimizeProgressFlag.update(if (it) 0.0f else 1.0f)
        }

        val repositionListener = {
            updatePreDrag(null)
        }
        dockingHSetting.listeners.add(repositionListener)
        dockingVSetting.listeners.add(repositionListener)
    }

    open val draggableHeight get() = height

    override fun getRenderHeight(): Float {
        return (super.getRenderHeight() - draggableHeight) * renderMinimizeProgress + draggableHeight
    }

    override val resizable = true
    open val minimizable = false

    open fun onResize() {}
    open fun onReposition() {}

    override fun onTick() {
        super.onTick()
        if (mouseState != MouseState.DRAG) {
            updatePreDrag(null)
        }
    }

    override fun onDisplayed() {
        lastActiveTime = System.currentTimeMillis() + 1000L
        super.onDisplayed()
        if (!minimized) {
            minimized = true
            minimized = false
        }
        updatePreDrag(null)
    }

    override fun onMouseInput(mousePos: Long) {
        super.onMouseInput(mousePos)
        if (mouseState != MouseState.DRAG) {
            updatePreDrag(Vec2f(Vec2f.getX(mousePos) - posX, Vec2f.getY(mousePos) - posY))
        }
    }

    override fun onClick(mousePos: Long, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        lastActiveTime = System.currentTimeMillis()
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        lastActiveTime = System.currentTimeMillis()
        if (minimizable && prevState != MouseState.DRAG && buttonId == 1 && Vec2f.getY(mousePos) - posY < draggableHeight) {
            minimized = !minimized
        }
        if (mouseState != MouseState.DRAG) {
            updatePreDrag(Vec2f(Vec2f.getX(mousePos) - posX, Vec2f.getY(mousePos) - posY))
        }
        if (screen.windows.runSynchronized { contains(dockingOverlay) }) {
            dockingOverlay.onRelease(mousePos, clickPos, buttonId)
        }
    }

    private fun updatePreDrag(mousePos: Vec2f?) {
        mousePos?.let { preDragMousePos = it.unbox() }
        preDragPos = Vec2f(posX, posY)
        preDragSize = Vec2f(width, height)
    }

    override fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        when (buttonId) {
            0 -> handleLeftClickDrag(clickPos, mousePos)
            1 -> handleRightClickDrag(clickPos, mousePos)
        }
    }

    private fun handleLeftClickDrag(clickPos: Long, mousePos: Long) {
        val relativeClickPos = Vec2f.minus(clickPos, preDragPos)
        val centerSplitterH = min(10.0, Vec2f.getX(preDragSize).toDouble() / 3.0)
        val centerSplitterV = min(10.0, Vec2f.getY(preDragSize).toDouble() / 3.0)

        val xRel = Vec2f.getX(relativeClickPos).toDouble()
        val horizontalSide = when {
            xRel in -2.0..centerSplitterH -> HAlign.LEFT
            xRel in centerSplitterH..(Vec2f.getX(preDragSize).toDouble() - centerSplitterH) -> HAlign.CENTER
            xRel in (Vec2f.getX(preDragSize).toDouble() - centerSplitterH)..(Vec2f.getX(preDragSize).toDouble() + 2.0) -> HAlign.RIGHT
            else -> null
        }

        val centerSplitterVCenter = if (draggableHeight != height && horizontalSide == HAlign.CENTER) 2.5 else min(15.0, Vec2f.getX(preDragSize).toDouble() / 3.0)
        val yRel = Vec2f.getY(relativeClickPos).toDouble()
        val verticalSide = when {
            yRel in -2.0..centerSplitterVCenter -> VAlign.TOP
            yRel in centerSplitterVCenter..(Vec2f.getY(preDragSize).toDouble() - centerSplitterV) -> VAlign.CENTER
            yRel in (Vec2f.getY(preDragSize).toDouble() - centerSplitterV)..(Vec2f.getY(preDragSize).toDouble() + 2.0) -> VAlign.BOTTOM
            else -> null
        }

        if (horizontalSide == null || verticalSide == null) return

        val draggedDist = Vec2f.minus(mousePos, clickPos)
        if (resizable && !minimized && (horizontalSide != HAlign.CENTER || verticalSide != VAlign.CENTER)) {
            handleResizeX(horizontalSide, draggedDist)
            handleResizeY(verticalSide, draggedDist)
            onResize()
        } else if (draggableHeight == height || Vec2f.getY(relativeClickPos) <= draggableHeight) {
            val x = (Vec2f.getX(preDragPos) + Vec2f.getX(draggedDist)).coerceIn(0.0f, mc.displayWidth.toFloat() / dev.wizard.meta.module.modules.client.ClickGUI.scaleFactor - width - 1.0f)
            val y = (Vec2f.getY(preDragPos) + Vec2f.getY(draggedDist)).coerceIn(0.0f, mc.displayHeight.toFloat() / dev.wizard.meta.module.modules.client.ClickGUI.scaleFactor - height - 1.0f)
            posX = x
            posY = y
            onReposition()
        }
    }

    private fun handleRightClickDrag(clickPos: Long, mousePos: Long) {
        val relativeClickPos = Vec2f.minus(clickPos, preDragPos)
        if (Vec2f.getY(relativeClickPos) > draggableHeight) return
        screen.displayWindow(dockingOverlay)
    }

    private fun handleResizeX(horizontalSide: HAlign, draggedDist: Long) {
        when (horizontalSide) {
            HAlign.LEFT -> {
                val draggedX = max(Vec2f.getX(draggedDist), 1.0f - Vec2f.getX(preDragPos))
                var newWidth = max(Vec2f.getX(preDragSize) - draggedX, minWidth)
                if (maxWidth != -1.0f) newWidth = min(newWidth, maxWidth)
                newWidth = min(newWidth, scaledDisplayWidth - 2.0f)
                val prevWidth = width
                width = newWidth
                posX += (prevWidth - newWidth)
            }
            HAlign.RIGHT -> {
                val draggedX = min(Vec2f.getX(draggedDist), Vec2f.getX(preDragPos) + Vec2f.getX(preDragSize) - 1.0f)
                var newWidth = max(Vec2f.getX(preDragSize) + draggedX, minWidth)
                if (maxWidth != -1.0f) newWidth = min(newWidth, maxWidth)
                newWidth = min(newWidth, scaledDisplayWidth - posX - 2.0f)
                width = newWidth
            }
            else -> {}
        }
    }

    private fun handleResizeY(verticalSide: VAlign, draggedDist: Long) {
        when (verticalSide) {
            VAlign.TOP -> {
                val draggedY = max(Vec2f.getY(draggedDist), 1.0f - Vec2f.getY(preDragPos))
                var newHeight = max(Vec2f.getY(preDragSize) - draggedY, minHeight)
                if (maxHeight != -1.0f) newHeight = min(newHeight, maxHeight)
                newHeight = min(newHeight, scaledDisplayHeight - 2.0f)
                val prevHeight = height
                height = newHeight
                posY += (prevHeight - newHeight)
            }
            VAlign.BOTTOM -> {
                val draggedY = min(Vec2f.getY(draggedDist), Vec2f.getY(preDragPos) + Vec2f.getY(preDragSize) - 1.0f)
                var newHeight = max(Vec2f.getY(preDragSize) + draggedY, minHeight)
                if (maxHeight != -1.0f) newHeight = min(newHeight, maxHeight)
                newHeight = min(newHeight, scaledDisplayHeight - posY - 2.0f)
                height = newHeight
            }
            else -> {}
        }
    }

    fun isInWindow(mousePos: Long): Boolean {
        if (!visible) return false
        val xMin = Vec2f.getX(preDragPos) - 2.0f
        val xMax = Vec2f.getX(preDragPos) + Vec2f.getX(preDragSize) + 2.0f
        val yMin = Vec2f.getY(preDragPos) - 2.0f
        val yMax = Vec2f.getY(preDragPos) + max(Vec2f.getY(preDragSize) * renderMinimizeProgress, draggableHeight) + 2.0f
        val x = Vec2f.getX(mousePos)
        val y = Vec2f.getY(mousePos)
        return x in xMin..xMax && y in yMin..yMax
    }
}
