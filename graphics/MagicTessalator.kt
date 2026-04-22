package dev.wizard.meta.graphics

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.MiscKt
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ActiveRenderInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

object MagicTessalator : Tessellator(0x200000) {
    private val mc: Minecraft = Minecraft.getMinecraft()

    fun prepareGL() {
        GlStateManager.pushMatrix()
        GL11.glLineWidth(1.0f)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(34383) // GL_MULTISAMPLE
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GlStateManager.disableAlpha()
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
        GlStateManager.enableCull()
        GlStateManager.enableBlend()
        GlStateManager.depthMask(false)
        GlStateManager.disableTexture2D()
        GlStateManager.disableLighting()
    }

    fun releaseGL() {
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.disableCull()
        GlStateManager.shadeModel(GL11.GL_FLAT)
        GlStateManager.enableAlpha()
        GlStateManager.depthMask(true)
        GL11.glDisable(34383) // GL_MULTISAMPLE
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        GL11.glLineWidth(1.0f)
        GlStateManager.popMatrix()
    }

    fun begin(mode: Int) {
        buffer.begin(mode, DefaultVertexFormats.POSITION_COLOR)
    }

    fun render() {
        draw()
    }

    @JvmStatic
    fun pTicks(): Float {
        return if (mc.isGamePaused) {
            MiscKt.renderPartialTicksPaused
        } else {
            mc.renderPartialTicks
        }
    }

    val camPos: Vec3d
        get() {
            var entity = mc.renderViewEntity
            if (entity == null) {
                entity = mc.player
            }
            val interpPos = EntityUtils.getInterpolatedPos(entity!!, pTicks())
            return interpPos.add(ActiveRenderInfo.getCameraPosition())
        }

    fun drawBox(box: AxisAlignedBB, color: ColorRGB, a: Int, sides: Int) {
        val vertexList = ArrayList<Vec3d>()
        if ((sides and 1) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minZ, box.maxZ, box.minY, EnumFacing.DOWN).toQuad())
        }
        if ((sides and 2) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minZ, box.maxZ, box.maxY, EnumFacing.UP).toQuad())
        }
        if ((sides and 4) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minZ, box.minZ, box.minY, EnumFacing.NORTH).toQuad())
        }
        if ((sides and 8) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minY, box.maxY, box.maxZ, EnumFacing.SOUTH).toQuad())
        }
        if ((sides and 0x10) != 0) {
            vertexList.addAll(SquareVec(box.minY, box.maxY, box.minZ, box.maxZ, box.minX, EnumFacing.WEST).toQuad())
        }
        if ((sides and 0x20) != 0) {
            vertexList.addAll(SquareVec(box.minY, box.maxY, box.minZ, box.maxZ, box.maxX, EnumFacing.EAST).toQuad())
        }
        for (pos in vertexList) {
            buffer.pos(pos.x, pos.y, pos.z).color(color.r, color.g, color.b, a).endVertex()
        }
    }

    fun drawLineTo(position: Vec3d, color: ColorRGB, a: Int, thickness: Float) {
        GlStateManager.glLineWidth(thickness)
        val cam = camPos
        buffer.pos(cam.x, cam.y, cam.z).color(color.r, color.g, color.b, a).endVertex()
        buffer.pos(position.x, position.y, position.z).color(color.r, color.g, color.b, a).endVertex()
    }

    fun drawOutline(box: AxisAlignedBB, color: ColorRGB, a: Int, sides: Int, thickness: Float) {
        val vertexList = LinkedHashSet<Pair<Vec3d, Vec3d>>()
        GlStateManager.glLineWidth(thickness)
        if ((sides and 1) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minZ, box.maxZ, box.minY, EnumFacing.DOWN).toLines())
        }
        if ((sides and 2) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minZ, box.maxZ, box.maxY, EnumFacing.UP).toLines())
        }
        if ((sides and 4) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minY, box.maxY, box.minZ, EnumFacing.NORTH).toLines())
        }
        if ((sides and 8) != 0) {
            vertexList.addAll(SquareVec(box.minX, box.maxX, box.minY, box.maxY, box.maxZ, EnumFacing.SOUTH).toLines())
        }
        if ((sides and 0x10) != 0) {
            vertexList.addAll(SquareVec(box.minY, box.maxY, box.minZ, box.maxZ, box.minX, EnumFacing.WEST).toLines())
        }
        if ((sides and 0x20) != 0) {
            vertexList.addAll(SquareVec(box.minY, box.maxY, box.minZ, box.maxZ, box.maxX, EnumFacing.EAST).toLines())
        }
        for (pair in vertexList) {
            val p1 = pair.first
            val p2 = pair.second
            buffer.pos(p1.x, p1.y, p1.z).color(color.r, color.g, color.b, a).endVertex()
            buffer.pos(p2.x, p2.y, p2.z).color(color.r, color.g, color.b, a).endVertex()
        }
    }

    private data class SquareVec(
        val minX: Double,
        val maxX: Double,
        val minZ: Double,
        val maxZ: Double,
        val y: Double,
        val facing: EnumFacing
    ) {
        fun toLines(): Array<Pair<Vec3d, Vec3d>> {
            val quad = toQuad()
            return arrayOf(
                quad[0] to quad[1],
                quad[1] to quad[2],
                quad[2] to quad[3],
                quad[3] to quad[0]
            )
        }

        fun toQuad(): Array<Vec3d> {
            return if (facing.horizontalIndex != -1) {
                val quad = to2DQuad()
                Array(4) { i ->
                    val vec = quad[i]
                    if (facing.axis == EnumFacing.Axis.X) {
                        Vec3d(vec.y, vec.x, vec.z)
                    } else {
                        Vec3d(vec.x, vec.z, vec.y)
                    }
                }
            } else {
                to2DQuad()
            }
        }

        fun to2DQuad(): Array<Vec3d> {
            return arrayOf(
                Vec3d(minX, y, minZ),
                Vec3d(minX, y, maxZ),
                Vec3d(maxX, y, maxZ),
                Vec3d(maxX, y, minZ)
            )
        }
    }
}