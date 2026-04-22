package dev.wizard.meta.gui

import dev.fastmc.common.collection.FastObjectArrayList
import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.*
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen.Companion.forEachWindow
import dev.wizard.meta.gui.rgui.MouseState
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.gui.rgui.windows.ListWindow
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.accessor.listShaders
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.state.TimedFlag
import dev.wizard.meta.util.threads.runSafeSuspend
import dev.wizard.meta.util.threads.runSynchronized
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

abstract class AbstractTrollGui : GuiScreen(), IListenerOwner by ListenerOwner(), IGuiScreen {
    override var mouseState = MouseState.NONE
    abstract val alwaysTicking: Boolean
    override val windows = ObjectLinkedOpenHashSet<WindowComponent>()
    override val windowsCachedList = FastObjectArrayList<WindowComponent>()
    override var lastClicked: WindowComponent? = null
    override var hovered: WindowComponent? = null
        get() {
            if (mouseState != MouseState.NONE) return field
            val value = windows.lastOrNull { it.isInWindow(mousePos) }
            if (value != field) {
                field?.onLeave(mousePos)
                value?.onHover(mousePos)
                field = value
            }
            return value
        }

    private var lastEventButton = -1
    private var lastClickPos = Vec2f.ZERO
        set(value) {
            field = value
            lastClickTime = System.currentTimeMillis()
        }
    private var lastClickTime = 0L

    var searchString = ""
        set(value) {
            renderStringPosX.update(MainFontRenderer.getWidth(value, 2.0f))
            field = value
        }

    val searching get() = searchString.isNotEmpty()

    private val renderStringPosX = AnimationFlag(Easing.OUT_CUBIC, 250.0f)
    private val blurShader = ShaderHelper(ResourceLocation("shaders/post/kawase_blur_6.json"))
    private val displayed = TimedFlag(false)

    private val fadeMultiplier: Float
        get() = if (displayed.value) {
            if (ClickGUI.fadeInTime > 0.0f) {
                Easing.OUT_CUBIC.inc(Easing.toDelta(displayed.lastUpdateTime, ClickGUI.fadeInTime * 1000.0f))
            } else {
                1.0f
            }
        } else {
            if (ClickGUI.fadeOutTime > 0.0f) {
                Easing.OUT_CUBIC.dec(Easing.toDelta(displayed.lastUpdateTime, ClickGUI.fadeOutTime * 1000.0f))
            } else {
                0.0f
            }
        }

    init {
        this.mc = Wrapper.minecraft

        parallelListener<TickEvent.Pre> {
            blurShader.shader?.let {
                val multiplier = ClickGUI.backGroundBlur * fadeMultiplier
                for (shader in it.listShaders) {
                    shader.shaderManager.getShaderUniform("multiplier")?.set(multiplier)
                }
            }

            if (displayed.value || alwaysTicking) {
                coroutineScope {
                    forEachWindow {
                        launch {
                            it.onTick()
                        }
                    }
                }
            }
        }

        safeListener<Render2DEvent.Mc>(-69420) {
            if (!displayed.value && fadeMultiplier > 0.0f) {
                drawScreen(0, 0, mc.renderPartialTicks)
            }
        }
    }

    override fun isVisible(): Boolean {
        return mc.currentScreen == this || displayed.value
    }

    override fun getMousePos(): Long {
        return mousePos
    }

    open fun onDisplayed() {
        searchString = ""
        displayed.value = true
        forEachWindow {
            it.onGuiDisplayed()
        }
    }

    override fun initGui() {
        super.initGui()
        val scaledResolution = ScaledResolution(mc)
        width = scaledResolution.scaledWidth + 16
        height = scaledResolution.scaledHeight + 16
    }

    override fun onGuiClosed() {
        lastClicked = null
        hovered = null
        searchString = ""
        renderStringPosX.forceUpdate(0.0f)
        displayed.value = false
        forEachWindow {
            it.onGuiClosed()
        }
    }

    override fun handleMouseInput() {
        mousePos = calcMousePos(Mouse.getEventX(), Mouse.getEventY())
        val eventButton = Mouse.getEventButton()
        if (Mouse.getEventButtonState()) {
            lastClickPos = mousePos
            lastEventButton = eventButton
        } else if (eventButton != -1) {
            lastEventButton = -1
        }
        hovered?.onMouseInput(mousePos)
        super.handleMouseInput()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        mouseState = MouseState.CLICK
        hovered?.onClick(lastClickPos, mouseButton)
        lastClicked = hovered
        lastClicked?.let {
            windows.runSynchronized {
                addAndMoveToLast(it)
            }
        }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        hovered?.onRelease(mousePos, lastClickPos, state)
        mouseState = MouseState.NONE
        lastClicked?.let {
            windows.runSynchronized {
                addAndMoveToLast(it)
            }
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (Vec2f.length(mousePos - lastClickPos) < 4.0f || System.currentTimeMillis() - lastClickTime < 50L) {
            return
        }
        mouseState = MouseState.DRAG
        hovered?.onDrag(mousePos, lastClickPos, clickedMouseButton)
        lastClicked?.let {
            windows.runSynchronized {
                addAndMoveToLast(it)
            }
        }
    }

    override fun handleKeyboardInput() {
        super.handleKeyboardInput()
        val keyCode = Keyboard.getEventKey()
        val keyState = Keyboard.getEventKeyState()
        lastClicked?.onKeyInput(keyCode, keyState)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        val lastClicked = lastClicked
        if (lastClicked is ListWindow && lastClicked.keybordListening != null) {
            return
        }
        if (keyCode == 14 || keyCode == 211) {
            searchString = ""
        } else if (typedChar.isLetter() || typedChar == ' ') {
            searchString += typedChar
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        updateScreen()
        mc.mcProfiler.startSection("trollGui")
        mc.mcProfiler.startSection("pre")
        GlStateUtils.alpha(false)
        GlStateUtils.depth(false)
        GL11.glEnable(GL11.GL_BLEND)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1)
        val scaledResolution = ScaledResolution(mc)
        val multiplier = fadeMultiplier
        mc.mcProfiler.endStartSection("backGround")
        GlStateUtils.rescaleActual()
        drawBackground(partialTicks)
        mc.mcProfiler.endStartSection("windows")
        GlStateUtils.rescaleTroll()
        GlStateManager.translate(0.0f, -(Resolution.trollHeightF * (1.0f - multiplier)), 0.0f)
        drawWindows()
        drawTypedString()
        mc.mcProfiler.endStartSection("post")
        GlStateUtils.rescaleMc()
        GlStateManager.translate(0.0f, -(scaledResolution.scaledHeight.toFloat() * (1.0f - multiplier)), 0.0f)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateUtils.alpha(true)
        mc.mcProfiler.endSection()
        mc.mcProfiler.endSection()
        GL20.glUseProgram(0)
    }

    private fun drawBackground(partialTicks: Float) {
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1)
        GlStateManager.colorMask(false, false, false, true)
        RenderUtils2D.drawRectFilled(Resolution.widthF, Resolution.heightF, ColorRGB(0, 0, 0, 255))
        GlStateManager.colorMask(true, true, true, true)
        if (ClickGUI.backGroundBlur > 0.0f) {
            GlStateManager.pushMatrix()
            GL20.glUseProgram(0)
            blurShader.shader?.render(partialTicks)
            mc.framebuffer.bindFramebuffer(true)
            blurShader.getFrameBuffer("final")?.framebufferRender(mc.displayWidth, mc.displayHeight)
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1)
            GlStateManager.popMatrix()
        }
        if (ClickGUI.darkness > 0.0f) {
            val color = ColorRGB(0, 0, 0, (ClickGUI.darkness * 255.0f * fadeMultiplier).toInt())
            RenderUtils2D.drawRectFilled(Resolution.widthF, Resolution.heightF, color)
        }
    }

    private fun drawWindows() {
        mc.mcProfiler.startSection("pre")
        drawEachWindow {
            it.onRender(Vec2f(it.renderPosX, it.renderPosY))
        }
        mc.mcProfiler.endStartSection("post")
        drawEachWindow {
            it.onPostRender(Vec2f(it.renderPosX, it.renderPosY))
        }
        mc.mcProfiler.endSection()
    }

    private inline fun drawEachWindow(renderBlock: (WindowComponent) -> Unit) {
        forEachWindow {
            if (it.visible) {
                GlStateManager.pushMatrix()
                GlStateManager.translate(it.renderPosX, it.renderPosY, 0.0f)
                renderBlock(it)
                GlStateManager.popMatrix()
            }
        }
    }

    private fun drawTypedString() {
        if (searchString.isNotBlank() && System.currentTimeMillis() - renderStringPosX.time <= 5000L) {
            val posX = Resolution.trollWidthF / 2.0f - renderStringPosX.get() / 2.0f
            val posY = Resolution.trollHeightF / 2.0f - MainFontRenderer.getHeight(2.0f) / 2.0f
            var color = ClickGUI.text
            color = ColorRGB.alpha(color, Easing.IN_CUBIC.dec(Easing.toDelta(renderStringPosX.time, 5000.0f), 0.0f, 255.0f).toInt())
            MainFontRenderer.drawString(searchString, posX, posY, color, 2.0f, false)
        }
    }

    override fun doesGuiPauseGame(): Boolean = false

    companion object : AlwaysListening {
        var mousePos: Long = calcMousePos(Mouse.getX(), Mouse.getY())

        init {
            listener<RunGameLoopEvent.Tick> {
                mousePos = calcMousePos(Mouse.getX(), Mouse.getY())
            }
        }

        fun calcMousePos(x: Int, y: Int): Long {
            val scaleFactor = ClickGUI.scaleFactor
            return Vec2f((x.toFloat() / scaleFactor) - 1.0f, (Wrapper.minecraft.displayHeight - 1 - y).toFloat() / scaleFactor)
        }
    }
}
