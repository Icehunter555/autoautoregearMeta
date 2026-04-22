package dev.wizard.meta.graphics

import dev.wizard.meta.graphics.buffer.PersistentMappedVBO
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.shaders.Shader
import dev.wizard.meta.structs.Pos2Color
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import kotlin.math.*

object RenderUtils2D {
    private val mc: Minecraft = Wrapper.mc
    var vertexSize = 0

    fun drawItem(itemStack: ItemStack, x: Int, y: Int, text: String? = null, drawOverlay: Boolean = true) {
        GL20.glUseProgram(0)
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        RenderHelper.enableGUIStandardItemLighting()
        mc.renderItem.zLevel = 0.0f
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y)
        if (drawOverlay) {
            mc.renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, itemStack, x, y, text)
        }
        mc.renderItem.zLevel = 0.0f
        RenderHelper.disableStandardItemLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateUtils.depth(false)
        GlStateUtils.texture2d(true)
    }

    fun drawCircleOutline(center: Vec2f = Vec2f.ZERO, radius: Float, segments: Int = 0, lineWidth: Float = 1.0f, color: ColorRGB) {
        drawArcOutline(center, radius, 0f to 360f, segments, lineWidth, color)
    }

    fun drawCircleFilled(center: Vec2f = Vec2f.ZERO, radius: Float, segments: Int = 0, color: ColorRGB) {
        drawArcFilled(center, radius, 0f to 360f, segments, color)
    }

    fun drawArcOutline(center: Vec2f, radius: Float, angleRange: Pair<Float, Float>, segments: Int, lineWidth: Float, color: ColorRGB) {
        val arcVertices = getArcVertices(center, radius, angleRange, segments)
        drawLineLoop(arcVertices, lineWidth, color)
    }

    fun drawArcFilled(center: Vec2f, radius: Float, angleRange: Pair<Float, Float>, segments: Int, color: ColorRGB) {
        val arcVertices = getArcVertices(center, radius, angleRange, segments)
        drawTriangleFan(center, arcVertices, color)
    }

    fun drawRectOutline(width: Float, height: Float, lineWidth: Float = 1.0f, color: ColorRGB) {
        drawRectOutline(0f, 0f, width, height, lineWidth, color)
    }

    fun drawRoundedRectFilled(x: Float, y: Float, width: Float, height: Float, radius: Float = -1f, color: ColorRGB, segments: Int = 20) {
        val effectiveRadius = if (radius < 0f) min(width, height) / 4f else radius
        val clampedRadius = min(effectiveRadius, min(width, height) / 2f)
        if (clampedRadius <= 0f) {
            drawRectFilled(x, y, x + width, y + height, color)
            return
        }
        val right = x + width
        val bottom = y + height
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius, 360f)
        drawRectFilled(x + clampedRadius, y, right - clampedRadius, bottom, color)
        drawRectFilled(x, y + clampedRadius, x + clampedRadius, bottom - clampedRadius, color)
        drawRectFilled(right - clampedRadius, y + clampedRadius, right, bottom - clampedRadius, color)
        drawArcFilled(Vec2f(x + clampedRadius, y + clampedRadius), clampedRadius, 180f to 270f, seg, color)
        drawArcFilled(Vec2f(right - clampedRadius, y + clampedRadius), clampedRadius, 270f to 360f, seg, color)
        drawArcFilled(Vec2f(x + clampedRadius, bottom - clampedRadius), clampedRadius, 90f to 180f, seg, color)
        drawArcFilled(Vec2f(right - clampedRadius, bottom - clampedRadius), clampedRadius, 0f to 90f, seg, color)
    }

    fun drawCapsuleRectFilled(x: Float, y: Float, x2: Float, y2: Float, color: ColorRGB, segments: Int = 50) {
        val radius = min(y2 / 2f, x2 / 2f)
        val right = x + x2
        val bottom = y + y2
        drawRectFilled(x + radius, y, right - radius, bottom, color)
        drawArcFilled(Vec2f(x + radius, y + radius), radius, 180f to 360f, segments, color)
        drawArcFilled(Vec2f(right - radius, y + radius), radius, 0f to 180f, segments, color)
    }

    fun drawRectOutline(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float = 1.0f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)
        putVertex(x1, y2, color)
        putVertex(x1, y1, color)
        putVertex(x2, y1, color)
        putVertex(x2, y2, color)
        draw(GL11.GL_LINE_LOOP)
        releaseGL()
    }

    fun drawRectFilled(x1: Float, y1: Float, x2: Float, y2: Float, color: ColorRGB) {
        prepareGL()
        putVertex(x1, y2, color)
        putVertex(x2, y2, color)
        putVertex(x2, y1, color)
        putVertex(x1, y1, color)
        draw(GL11.GL_QUADS)
        releaseGL()
    }

    fun drawTriangleFan(center: Vec2f, vertices: Array<Vec2f>, color: ColorRGB) {
        prepareGL()
        putVertex(center, color)
        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL11.GL_TRIANGLE_FAN)
        releaseGL()
    }

    fun drawLineLoop(vertices: Array<Vec2f>, lineWidth: Float, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)
        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL11.GL_LINE_LOOP)
        releaseGL()
        GlStateManager.glLineWidth(1.0f)
    }

    fun putVertex(pos: Vec2f, color: ColorRGB) {
        putVertex(pos.x, pos.y, color)
    }

    fun putVertex(posX: Float, posY: Float, color: ColorRGB) {
        val array = PersistentMappedVBO.arr
        val struct = Pos2Color(array)
        struct.pos.x = posX
        struct.pos.y = posY
        struct.color = color.rgba
        array.plusAssign(Pos2Color.size)
        vertexSize++
    }

    fun draw(mode: Int) {
        if (vertexSize == 0) return
        DrawShader.bind()
        GL30.glBindVertexArray(PersistentMappedVBO.POS2_COLOR)
        GL11.glDrawArrays(mode, PersistentMappedVBO.drawOffset, vertexSize)
        PersistentMappedVBO.end()
        GL30.glBindVertexArray(0)
        vertexSize = 0
    }

    private fun getArcVertices(center: Vec2f, radius: Float, angleRange: Pair<Float, Float>, segments: Int): Array<Vec2f> {
        val range = max(angleRange.first, angleRange.second) - min(angleRange.first, angleRange.second)
        val seg = if (segments > 0) segments else calculateOptimalSegments(radius, range)
        val segAngle = range / seg
        return Array(seg + 1) { i ->
            val angle = Math.toRadians((i * segAngle + angleRange.first).toDouble()).toFloat()
            Vec2f(center.x + radius * sin(angle), center.y - radius * cos(angle))
        }
    }

    private fun calculateOptimalSegments(radius: Float, angleRange: Float): Int {
        val segments = (radius * 0.5f * PI * (angleRange / 360f)).roundToInt()
        val rangeProportion = angleRange / 360f
        val minSeg = max((8 * rangeProportion).toInt(), 4)
        val maxSeg = max((64 * rangeProportion).toInt(), 16)
        return segments.coerceIn(minSeg, maxSeg)
    }

    fun prepareGL() {
        GlStateUtils.alpha(false)
        GlStateUtils.blend(true)
        GlStateUtils.lineSmooth(true)
        GlStateUtils.cull(false)
    }

    fun releaseGL() {
        GlStateUtils.lineSmooth(false)
        GlStateUtils.cull(true)
    }

    private object DrawShader : Shader("/assets/meta/shaders/general/Pos2Color.vsh", "/assets/meta/shaders/general/Pos2Color.fsh")
}