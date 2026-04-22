package dev.wizard.meta.graphics

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

object GLUtil {
    fun matrix(block: () -> Unit) {
        GL11.glPushMatrix()
        block()
        GL11.glPopMatrix()
    }

    fun renderGL(block: () -> Unit) {
        MagicTessalator.prepareGL()
        block()
        MagicTessalator.releaseGL()
    }

    fun draw(mode: Int, block: () -> Unit) {
        GL11.glBegin(mode)
        block()
        GL11.glEnd()
    }

    fun withScale(scale: Double, block: () -> Unit) {
        GL11.glScaled(scale, scale, scale)
        block()
        GL11.glScaled(1.0 / scale, 1.0 / scale, 1.0 / scale)
    }

    fun prepareGL2D() {
        GlStateManager.disableAlpha()
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.shadeModel(7425)
        GL11.glEnable(2848)
        GL11.glHint(3154, 4354)
        GlStateManager.disableCull()
    }

    fun releaseGL2D() {
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.shadeModel(7424)
        GL11.glDisable(2848)
        GlStateManager.enableCull()
    }

    fun glColor(color: Color) {
        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        GlStateManager.color(red, green, blue, color.alpha / 255f)
    }

    fun Vec3d.translateGL(): Vec3d {
        GL11.glTranslated(x, y, z)
        return this
    }

    fun Vec3d.glVertex(): Vec3d {
        GL11.glVertex3d(x, y, z)
        return this
    }
}
