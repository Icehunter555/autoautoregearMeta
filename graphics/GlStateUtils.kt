package dev.wizard.meta.graphics

import dev.wizard.meta.util.Quad
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

object GlStateUtils {
    private val mc: Minecraft = Wrapper.mc
    private var lastScissor: Quad<Int, Int, Int, Int>? = null
    private val scissorList = ArrayList<Quad<Int, Int, Int, Int>>()

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        lastScissor = Quad(x, y, width, height)
        GL11.glScissor(x, y, width, height)
    }

    fun pushScissor() {
        lastScissor?.let { scissorList.add(it) }
    }

    fun popScissor() {
        scissorList.removeLastOrNull()?.let {
            scissor(it.first, it.second, it.third, it.fourth)
        }
    }

    fun useVbo(): Boolean = mc.gameSettings.useVbo

    fun alpha(state: Boolean) {
        if (state) GlStateManager.enableAlpha() else GlStateManager.disableAlpha()
    }

    fun blend(state: Boolean) {
        if (state) GlStateManager.enableBlend() else GlStateManager.disableBlend()
    }

    fun smooth(state: Boolean) {
        if (state) GlStateManager.shadeModel(GL11.GL_SMOOTH) else GlStateManager.shadeModel(GL11.GL_FLAT)
    }

    fun lineSmooth(state: Boolean) {
        if (state) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        } else {
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
        }
    }

    fun depth(state: Boolean) {
        if (state) GlStateManager.enableDepth() else GlStateManager.disableDepth()
    }

    fun texture2d(state: Boolean) {
        if (state) GlStateManager.enableTexture2D() else GlStateManager.disableTexture2D()
    }

    fun cull(state: Boolean) {
        if (state) GlStateManager.enableCull() else GlStateManager.disableCull()
    }

    fun lighting(state: Boolean) {
        if (state) GlStateManager.enableLighting() else GlStateManager.disableLighting()
    }

    fun rescaleActual() {
        rescale(mc.displayWidth.toDouble(), mc.displayHeight.toDouble())
    }

    fun rescaleTroll() {
        rescale(Resolution.trollWidthF.toDouble(), Resolution.trollHeightF.toDouble())
    }

    fun rescaleMc() {
        val resolution = ScaledResolution(mc)
        rescale(resolution.scaledWidth_double, resolution.scaledHeight_double)
    }

    fun pushMatrixAll() {
        GlStateManager.matrixMode(5889)
        GlStateManager.pushMatrix()
        GlStateManager.matrixMode(5888)
        GlStateManager.pushMatrix()
    }

    fun popMatrixAll() {
        GlStateManager.matrixMode(5889)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(5888)
        GlStateManager.popMatrix()
    }

    fun rescale(width: Double, height: Double) {
        GlStateManager.clear(256)
        GlStateManager.matrixMode(5889)
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, width, height, 0.0, 1000.0, 3000.0)
        GlStateManager.matrixMode(5888)
        GlStateManager.loadIdentity()
        GlStateManager.translate(0.0f, 0.0f, -2000.0f)
    }
}