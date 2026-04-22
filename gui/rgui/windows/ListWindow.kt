package dev.wizard.meta.gui.rgui.windows

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.InteractiveComponent
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.module.modules.render.AntiAlias
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import kotlin.math.*

open class ListWindow(
    screen: IGuiScreen,
    name: CharSequence,
    saveToConfig: Component.UiSettingGroup,
    vararg childrenIn: Component
) : TitledWindow(screen, name, saveToConfig) {

    val children = arrayListOf<Component>()
    override val minHeight = 100.0f
    var hoveredChild: Component? = null
        private set(value) {
            if (value == field) return
            (field as? InteractiveComponent)?.onLeave(screen.mousePos)
            (value as? InteractiveComponent)?.onHover(screen.mousePos)
            field = value
        }

    private val scrollTimer = TickTimer()
    private var lastScrollSpeedUpdate = System.currentTimeMillis()
    var scrollSpeed = 0.0f
    var scrollProgress = 0.0f
    private var doubleClickTime = -1L

    val optimalWidth by FrameFloat {
        val result = children.asSequence().map { it.minWidth + ClickGUI.xMargin * 2.0f }.maxOrNull() ?: 80.0f
        max(result, max(dev.wizard.meta.graphics.font.renderer.MainFontRenderer.getWidth(name) + 20.0f, 80.0f))
    }

    val optimalHeight by FrameFloat {
        val sum = children.asSequence().filter { it.visible }.sumOf { (it.height + ClickGUI.yMargin).toDouble() }.toFloat()
        sum + draggableHeight + max(ClickGUI.xMargin, ClickGUI.yMargin)
    }

    init {
        children.addAll(childrenIn)
        updateChildPosSize()
    }

    override val minWidth = 80.0f
    override val maxWidth get() = max(minWidth, 200.0f)
    override val maxHeight get() = mc.displayHeight.toFloat() / ClickGUI.scaleFactor

    override val resizable get() = hoveredChild == null

    override fun onDisplayed() {
        super.onDisplayed()
        lastScrollSpeedUpdate = System.currentTimeMillis()
        onTick()
        children.forEach { it.onDisplayed() }
        updateChildPosSize()
        (optimalWidth as FrameFloat).updateLazy()
        (optimalHeight as FrameFloat).updateLazy()
    }

    override fun onClosed() {
        super.onClosed()
        children.forEach { it.onClosed() }
    }

    override fun onResize() {
        super.onResize()
        updateChildPosSize()
    }

    override fun onTick() {
        super.onTick()
        children.forEach { it.onTick() }
        updateChildPosSize()
        if (mouseState != MouseState.DRAG) {
            updateHovered(Vec2f(Vec2f.getX(screen.mousePos) - posX, Vec2f.getY(screen.mousePos) - posY))
        }
    }

    override fun onRender(absolutePos: Long) {
        super.onRender(absolutePos)
        updateScrollProgress()
        if (renderMinimizeProgress != 0.0f) {
            renderChildren { child ->
                child.onRender(Vec2f.plus(absolutePos, child.renderPosX, child.renderPosY - scrollProgress))
            }
        }
    }

    override fun onPostRender(absolutePos: Long) {
        super.onPostRender(absolutePos)
        if (renderMinimizeProgress != 0.0f) {
            renderChildren { child ->
                child.onPostRender(Vec2f.plus(absolutePos, child.renderPosX, child.renderPosY - scrollProgress))
            }
        }
    }

    private inline fun renderChildren(renderBlock: (Component) -> Unit) {
        val sampleLevel = AntiAlias.sampleLevel
        GlStateUtils.scissor(
            MathUtilKt.floorToInt(((renderPosX + ClickGUI.xMargin) * ClickGUI.scaleFactor - 0.5f) * sampleLevel),
            MathUtilKt.floorToInt(mc.displayHeight * sampleLevel - ((renderPosY * sampleLevel + renderHeight * sampleLevel) * ClickGUI.scaleFactor - 0.5f)),
            MathUtilKt.ceilToInt(((renderWidth - ClickGUI.xMargin * 2.0f) * ClickGUI.scaleFactor + 1.0f) * sampleLevel),
            MathUtilKt.ceilToInt((renderHeight - draggableHeight) * ClickGUI.scaleFactor * sampleLevel)
        )
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GlStateManager.pushMatrix()
        GlStateManager.translate(0.0f, -scrollProgress, 0.0f)
        mc.mcProfiler.startSection("childrens")
        for (child in children) {
            if (!child.visible || child.renderPosY + child.renderHeight - scrollProgress < draggableHeight || child.renderPosY - scrollProgress > renderHeight) continue
            GlStateManager.pushMatrix()
            GlStateManager.translate(child.renderPosX, child.renderPosY, 0.0f)
            renderBlock(child)
            GlStateManager.popMatrix()
        }
        mc.mcProfiler.endSection()
        GlStateManager.popMatrix()
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    private fun updateChildPosSize() {
        (optimalWidth as FrameFloat).updateLazy()
        (optimalHeight as FrameFloat).updateLazy()
        var y = (if (draggableHeight != height) draggableHeight else 0.0f) + ClickGUI.yMargin
        for (child in children) {
            child.posX = ClickGUI.xMargin
            child.posY = y
            child.width = width - ClickGUI.xMargin * 2.0f
            if (child.visible) {
                y += child.height + ClickGUI.yMargin
            }
        }
        (optimalWidth as FrameFloat).updateLazy()
        (optimalHeight as FrameFloat).updateLazy()
    }

    private fun updateScrollProgress() {
        if (children.isEmpty()) return
        val x = (System.currentTimeMillis() - lastScrollSpeedUpdate).toDouble() / 100.0
        val lnHalf = log(0.25, E)
        val newSpeed = scrollSpeed.toDouble() * 0.25.pow(x)
        scrollProgress += (newSpeed / lnHalf - scrollSpeed.toDouble() / lnHalf).toFloat()
        scrollSpeed = newSpeed.toFloat()
        lastScrollSpeedUpdate = System.currentTimeMillis()

        if (scrollTimer.tick(100L)) {
            val lastVisible = children.lastOrNull { it.visible }
            val maxScrollProgress = if (lastVisible != null) {
                max(lastVisible.posY + lastVisible.height + ClickGUI.yMargin - height, 0.01f)
            } else {
                draggableHeight
            }

            if (scrollProgress < 0.0f) {
                scrollSpeed = scrollProgress * -0.4f
            } else if (scrollProgress > maxScrollProgress) {
                scrollSpeed = (scrollProgress - maxScrollProgress) * -0.4f
            }
        }
    }

    override fun onMouseInput(mousePos: Long) {
        super.onMouseInput(mousePos)
        val relativeMousePos = Vec2f.minus(mousePos, posX, posY)
        if (Mouse.getEventDWheel() != 0) {
            scrollTimer.reset()
            scrollSpeed -= Mouse.getEventDWheel().toFloat() * 0.2f
        }
        if (mouseState != MouseState.DRAG) {
            updateHovered(relativeMousePos)
        }
        if (!minimized) {
            (hoveredChild as? InteractiveComponent)?.onMouseInput(getRelativeMousePos(mousePos, hoveredChild as InteractiveComponent))
        }
    }

    private fun updateHovered(relativeMousePos: Long) {
        if (minimized || mouseState == MouseState.NONE) {
            hoveredChild = null
            return
        }
        val yRel = Vec2f.getY(relativeMousePos)
        val xRel = Vec2f.getX(relativeMousePos)
        if (yRel < draggableHeight || xRel < ClickGUI.xMargin || xRel > renderWidth - ClickGUI.xMargin) {
            hoveredChild = null
        } else {
            hoveredChild = children.firstOrNull { 
                it.visible && yRel + scrollProgress in it.posY..(it.posY + it.height)
            }
        }
    }

    override fun onLeave(mousePos: Long) {
        super.onLeave(mousePos)
        hoveredChild = null
    }

    override fun onClick(mousePos: Long, buttonId: Int) {
        super.onClick(mousePos, buttonId)
        val relativeMousePos = Vec2f.minus(mousePos, posX, posY)
        updateHovered(relativeMousePos)
        handleDoubleClick(mousePos, buttonId)
        if (!minimized) {
            (hoveredChild as? InteractiveComponent)?.onClick(getRelativeMousePos(mousePos, hoveredChild as InteractiveComponent), buttonId)
        }
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (!minimized) {
            (hoveredChild as? InteractiveComponent)?.onRelease(getRelativeMousePos(mousePos, hoveredChild as InteractiveComponent), clickPos, buttonId)
        }
    }

    override fun onDrag(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onDrag(mousePos, clickPos, buttonId)
        if (!minimized) {
            val child = hoveredChild as? InteractiveComponent ?: return
            child.onDrag(getRelativeMousePos(mousePos, child), getRelativeMousePos(clickPos, child), buttonId)
        }
    }

    override fun onKeyInput(keyCode: Int, keyState: Boolean) {
        super.onKeyInput(keyCode, keyState)
        if (!minimized) {
            (hoveredChild as? InteractiveComponent)?.onKeyInput(keyCode, keyState)
        }
    }

    private fun handleDoubleClick(mousePos: Long, buttonId: Int) {
        if (!visible || buttonId != 0 || Vec2f.getY(mousePos) - posY >= draggableHeight) {
            doubleClickTime = -1L
            return
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - doubleClickTime > 500L) {
            doubleClickTime = currentTime
        } else {
            if (optimalHeight >= height) {
                val maxHeight = scaledDisplayHeight - 2.0f
                height = min(optimalHeight, maxHeight)
                posY = min(posY, maxHeight - height)
            }
            doubleClickTime = -1L
        }
    }

    private fun getRelativeMousePos(mousePos: Long, component: InteractiveComponent): Long {
        return Vec2f.minus(Vec2f.minus(mousePos, posX, posY - scrollProgress), component.posX, component.posY)
    }
}
