package dev.wizard.meta.graphics

import dev.luna5ama.kmogus.MutableArr
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.ListenerKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.buffer.PersistentMappedVBO
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.shaders.Shader
import dev.wizard.meta.structs.Pos3Color
import dev.wizard.meta.util.accessor.RenderKt
import net.minecraft.client.renderer.ActiveRenderInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

object RenderUtils3D : AlwaysListening {
    var vertexSize = 0
    var translationX = 0.0
    var translationY = 0.0
    var translationZ = 0.0
    var partialTicks = 0f
        private set
    var camPos = Vec3d.ZERO
        private set

    init {
        ListenerKt.listener(this, RunGameLoopEvent.Tick::class.java) {
            SafeClientEvent.instance?.let { event ->
                partialTicks = if (event.mc.isGamePaused) {
                    event.mc.renderPartialTicks
                } else {
                    event.mc.renderPartialTicks
                }
            }
        }

        ListenerKt.listener(this, Render3DEvent::class.java) {
            SafeClientEvent.instance?.let { event ->
                val renderManager = event.mc.renderManager
                translationX = -RenderKt.renderPosX(renderManager)
                translationY = -RenderKt.renderPosY(renderManager)
                translationZ = -RenderKt.renderPosZ(renderManager)
            }
        }

        ListenerKt.listener(this, Render3DEvent::class.java, priority = Int.MAX_VALUE, alwaysListening = true) {
            SafeClientEvent.instance?.let { event ->
                val entity = event.mc.renderViewEntity ?: event.player
                val ticks = partialTicks
                val x = entity.prevPosX + (entity.posX - entity.prevPosX) * ticks.toDouble()
                val y = entity.prevPosY + (entity.posY - entity.prevPosY) * ticks.toDouble()
                val z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * ticks.toDouble()
                val camOffset = ActiveRenderInfo.getCameraPosition()
                camPos = Vec3d(x + camOffset.x, y + camOffset.y, z + camOffset.z)
            }
        }
    }

    fun setTranslation(x: Double, y: Double, z: Double) {
        translationX = x
        translationY = y
        translationZ = z
    }

    fun resetTranslation() {
        translationX = 0.0
        translationY = 0.0
        translationZ = 0.0
    }

    fun drawBox(box: AxisAlignedBB, color: ColorRGB, sides: Int) {
        if ((sides and 1) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, color)
            putVertex(box.minX, box.minY, box.minZ, color)
            putVertex(box.maxX, box.minY, box.minZ, color)
            putVertex(box.maxX, box.minY, box.maxZ, color)
        }
        if ((sides and 2) != 0) {
            putVertex(box.minX, box.maxY, box.minZ, color)
            putVertex(box.minX, box.maxY, box.maxZ, color)
            putVertex(box.maxX, box.maxY, box.maxZ, color)
            putVertex(box.maxX, box.maxY, box.minZ, color)
        }
        if ((sides and 4) != 0) {
            putVertex(box.minX, box.minY, box.minZ, color)
            putVertex(box.minX, box.maxY, box.minZ, color)
            putVertex(box.maxX, box.maxY, box.minZ, color)
            putVertex(box.maxX, box.minY, box.minZ, color)
        }
        if ((sides and 8) != 0) {
            putVertex(box.maxX, box.minY, box.maxZ, color)
            putVertex(box.maxX, box.maxY, box.maxZ, color)
            putVertex(box.minX, box.maxY, box.maxZ, color)
            putVertex(box.minX, box.minY, box.maxZ, color)
        }
        if ((sides and 0x10) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, color)
            putVertex(box.minX, box.maxY, box.maxZ, color)
            putVertex(box.minX, box.maxY, box.minZ, color)
            putVertex(box.minX, box.minY, box.minZ, color)
        }
        if ((sides and 0x20) != 0) {
            putVertex(box.maxX, box.minY, box.minZ, color)
            putVertex(box.maxX, box.maxY, box.minZ, color)
            putVertex(box.maxX, box.maxY, box.maxZ, color)
            putVertex(box.maxX, box.minY, box.maxZ, color)
        }
    }

    fun drawLineTo(position: Vec3d, color: ColorRGB) {
        putVertex(camPos.x, camPos.y, camPos.z, color)
        putVertex(position.x, position.y, position.z, color)
    }

    fun drawOutline(box: AxisAlignedBB, color: ColorRGB) {
        putVertex(box.minX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.maxZ, color)
        putVertex(box.maxX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.minZ, color)

        putVertex(box.minX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.maxZ, color)
        putVertex(box.maxX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.minZ, color)

        putVertex(box.minX, box.minY, box.minZ, color)
        putVertex(box.minX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.minZ, color)
        putVertex(box.maxX, box.maxY, box.minZ, color)
        putVertex(box.maxX, box.minY, box.maxZ, color)
        putVertex(box.maxX, box.maxY, box.maxZ, color)
        putVertex(box.minX, box.minY, box.maxZ, color)
        putVertex(box.minX, box.maxY, box.maxZ, color)
    }

    fun drawVerticalGradientBox(box: AxisAlignedBB, colorTop: ColorRGB, colorBottom: ColorRGB, sides: Int) {
        if ((sides and 1) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, colorBottom)
            putVertex(box.minX, box.minY, box.minZ, colorBottom)
            putVertex(box.maxX, box.minY, box.minZ, colorBottom)
            putVertex(box.maxX, box.minY, box.maxZ, colorBottom)
        }
        if ((sides and 2) != 0) {
            putVertex(box.minX, box.maxY, box.minZ, colorTop)
            putVertex(box.minX, box.maxY, box.maxZ, colorTop)
            putVertex(box.maxX, box.maxY, box.maxZ, colorTop)
            putVertex(box.maxX, box.maxY, box.minZ, colorTop)
        }
        if ((sides and 4) != 0) {
            putVertex(box.minX, box.minY, box.minZ, colorBottom)
            putVertex(box.minX, box.maxY, box.minZ, colorTop)
            putVertex(box.maxX, box.maxY, box.minZ, colorTop)
            putVertex(box.maxX, box.minY, box.minZ, colorBottom)
        }
        if ((sides and 8) != 0) {
            putVertex(box.maxX, box.minY, box.maxZ, colorBottom)
            putVertex(box.maxX, box.maxY, box.maxZ, colorTop)
            putVertex(box.minX, box.maxY, box.maxZ, colorTop)
            putVertex(box.minX, box.minY, box.maxZ, colorBottom)
        }
        if ((sides and 0x10) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, colorBottom)
            putVertex(box.minX, box.maxY, box.maxZ, colorTop)
            putVertex(box.minX, box.maxY, box.minZ, colorTop)
            putVertex(box.minX, box.minY, box.minZ, colorBottom)
        }
        if ((sides and 0x20) != 0) {
            putVertex(box.maxX, box.minY, box.minZ, colorBottom)
            putVertex(box.maxX, box.maxY, box.minZ, colorTop)
            putVertex(box.maxX, box.maxY, box.maxZ, colorTop)
            putVertex(box.maxX, box.minY, box.maxZ, colorBottom)
        }
    }

    fun drawHorizontalGradientBox(box: AxisAlignedBB, colorLeft: ColorRGB, colorRight: ColorRGB, sides: Int) {
        if ((sides and 1) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, colorLeft)
            putVertex(box.minX, box.minY, box.minZ, colorLeft)
            putVertex(box.maxX, box.minY, box.minZ, colorRight)
            putVertex(box.maxX, box.minY, box.maxZ, colorRight)
        }
        if ((sides and 2) != 0) {
            putVertex(box.minX, box.maxY, box.minZ, colorLeft)
            putVertex(box.minX, box.maxY, box.maxZ, colorLeft)
            putVertex(box.maxX, box.maxY, box.maxZ, colorRight)
            putVertex(box.maxX, box.maxY, box.minZ, colorRight)
        }
        if ((sides and 4) != 0) {
            putVertex(box.minX, box.minY, box.minZ, colorLeft)
            putVertex(box.minX, box.maxY, box.minZ, colorLeft)
            putVertex(box.maxX, box.maxY, box.minZ, colorRight)
            putVertex(box.maxX, box.minY, box.minZ, colorRight)
        }
        if ((sides and 8) != 0) {
            putVertex(box.maxX, box.minY, box.maxZ, colorRight)
            putVertex(box.maxX, box.maxY, box.maxZ, colorRight)
            putVertex(box.minX, box.maxY, box.maxZ, colorLeft)
            putVertex(box.minX, box.minY, box.maxZ, colorLeft)
        }
        if ((sides and 0x10) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, colorLeft)
            putVertex(box.minX, box.maxY, box.maxZ, colorLeft)
            putVertex(box.minX, box.maxY, box.minZ, colorLeft)
            putVertex(box.minX, box.minY, box.minZ, colorLeft)
        }
        if ((sides and 0x20) != 0) {
            putVertex(box.maxX, box.minY, box.minZ, colorRight)
            putVertex(box.maxX, box.maxY, box.minZ, colorRight)
            putVertex(box.maxX, box.maxY, box.maxZ, colorRight)
            putVertex(box.maxX, box.minY, box.maxZ, colorRight)
        }
    }

    fun drawTripleVerticalGradientBox(box: AxisAlignedBB, colorTop: ColorRGB, colorMiddle: ColorRGB, colorBottom: ColorRGB, sides: Int, progress: Float) {
        val height = box.maxY - box.minY
        val midY = (box.minY + box.maxY) / 2.0

        fun getColorAtY(y: Double): ColorRGB {
            val relativePos = ((y - box.minY) / height).coerceIn(0.0, 1.0)
            return if (relativePos < 0.333) {
                ColorRGB.lerp(colorBottom, colorMiddle, (relativePos * 3).toFloat())
            } else if (relativePos < 0.667) {
                ColorRGB.lerp(colorMiddle, colorTop, ((relativePos - 0.333) * 3).toFloat())
            } else {
                ColorRGB.lerp(colorTop, colorBottom, ((relativePos - 0.667) * 3).toFloat())
            }
        }

        if ((sides and 1) != 0) {
            val c = getColorAtY(box.minY)
            putVertex(box.minX, box.minY, box.maxZ, c)
            putVertex(box.minX, box.minY, box.minZ, c)
            putVertex(box.maxX, box.minY, box.minZ, c)
            putVertex(box.maxX, box.minY, box.maxZ, c)
        }
        if ((sides and 2) != 0) {
            val c = getColorAtY(box.maxY)
            putVertex(box.minX, box.maxY, box.minZ, c)
            putVertex(box.minX, box.maxY, box.maxZ, c)
            putVertex(box.maxX, box.maxY, box.maxZ, c)
            putVertex(box.maxX, box.maxY, box.minZ, c)
        }
        if ((sides and 4) != 0) {
            putVertex(box.minX, box.minY, box.minZ, getColorAtY(box.minY))
            putVertex(box.minX, midY, box.minZ, getColorAtY(midY))
            putVertex(box.maxX, midY, box.minZ, getColorAtY(midY))
            putVertex(box.maxX, box.minY, box.minZ, getColorAtY(box.minY))
            putVertex(box.minX, midY, box.minZ, getColorAtY(midY))
            putVertex(box.minX, box.maxY, box.minZ, getColorAtY(box.maxY))
            putVertex(box.maxX, box.maxY, box.minZ, getColorAtY(box.maxY))
            putVertex(box.maxX, midY, box.minZ, getColorAtY(midY))
        }
        if ((sides and 8) != 0) {
            putVertex(box.maxX, box.minY, box.maxZ, getColorAtY(box.minY))
            putVertex(box.maxX, midY, box.maxZ, getColorAtY(midY))
            putVertex(box.minX, midY, box.maxZ, getColorAtY(midY))
            putVertex(box.minX, box.minY, box.maxZ, getColorAtY(box.minY))
            putVertex(box.maxX, midY, box.maxZ, getColorAtY(midY))
            putVertex(box.maxX, box.maxY, box.maxZ, getColorAtY(box.maxY))
            putVertex(box.minX, box.maxY, box.maxZ, getColorAtY(box.maxY))
            putVertex(box.minX, midY, box.maxZ, getColorAtY(midY))
        }
        if ((sides and 0x10) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, getColorAtY(box.minY))
            putVertex(box.minX, midY, box.maxZ, getColorAtY(midY))
            putVertex(box.minX, midY, box.minZ, getColorAtY(midY))
            putVertex(box.minX, box.minY, box.minZ, getColorAtY(box.minY))
            putVertex(box.minX, midY, box.maxZ, getColorAtY(midY))
            putVertex(box.minX, box.maxY, box.maxZ, getColorAtY(box.maxY))
            putVertex(box.minX, box.maxY, box.minZ, getColorAtY(box.maxY))
            putVertex(box.minX, midY, box.minZ, getColorAtY(midY))
        }
        if ((sides and 0x20) != 0) {
            putVertex(box.maxX, box.minY, box.minZ, getColorAtY(box.minY))
            putVertex(box.maxX, midY, box.minZ, getColorAtY(midY))
            putVertex(box.maxX, midY, box.maxZ, getColorAtY(midY))
            putVertex(box.maxX, box.minY, box.maxZ, getColorAtY(box.minY))
            putVertex(box.maxX, midY, box.minZ, getColorAtY(midY))
            putVertex(box.maxX, box.maxY, box.minZ, getColorAtY(box.maxY))
            putVertex(box.maxX, box.maxY, box.maxZ, getColorAtY(box.maxY))
            putVertex(box.maxX, midY, box.maxZ, getColorAtY(midY))
        }
    }

    fun drawTripleHorizontalGradientBox(box: AxisAlignedBB, colorLeft: ColorRGB, colorMiddle: ColorRGB, colorRight: ColorRGB, sides: Int, progress: Float) {
        val width = box.maxX - box.minX
        val midX = (box.minX + box.maxX) / 2.0

        fun getColorAtX(x: Double): ColorRGB {
            val relativePos = ((x - box.minX) / width).coerceIn(0.0, 1.0)
            return if (relativePos < 0.333) {
                ColorRGB.lerp(colorLeft, colorMiddle, (relativePos * 3).toFloat())
            } else if (relativePos < 0.667) {
                ColorRGB.lerp(colorMiddle, colorRight, ((relativePos - 0.333) * 3).toFloat())
            } else {
                ColorRGB.lerp(colorRight, colorLeft, ((relativePos - 0.667) * 3).toFloat())
            }
        }

        if ((sides and 1) != 0) {
            putVertex(box.minX, box.minY, box.maxZ, getColorAtX(box.minX))
            putVertex(box.minX, box.minY, box.minZ, getColorAtX(box.minX))
            putVertex(box.maxX, box.minY, box.minZ, getColorAtX(box.maxX))
            putVertex(box.maxX, box.minY, box.maxZ, getColorAtX(box.maxX))
        }
        if ((sides and 2) != 0) {
            putVertex(box.minX, box.maxY, box.minZ, getColorAtX(box.minX))
            putVertex(box.minX, box.maxY, box.maxZ, getColorAtX(box.minX))
            putVertex(box.maxX, box.maxY, box.maxZ, getColorAtX(box.maxX))
            putVertex(box.maxX, box.maxY, box.minZ, getColorAtX(box.maxX))
        }
        if ((sides and 4) != 0) {
            putVertex(box.minX, box.minY, box.minZ, getColorAtX(box.minX))
            putVertex(box.minX, box.maxY, box.minZ, getColorAtX(box.minX))
            putVertex(box.maxX, box.maxY, box.minZ, getColorAtX(box.maxX))
            putVertex(box.maxX, box.minY, box.minZ, getColorAtX(box.maxX))
        }
        if ((sides and 8) != 0) {
            putVertex(midX, box.minY, box.maxZ, getColorAtX(midX))
            putVertex(midX, box.maxY, box.maxZ, getColorAtX(midX))
            putVertex(box.minX, box.maxY, box.maxZ, getColorAtX(box.minX))
            putVertex(box.minX, box.minY, box.maxZ, getColorAtX(box.minX))
            putVertex(box.maxX, box.minY, box.maxZ, getColorAtX(box.maxX))
            putVertex(box.maxX, box.maxY, box.maxZ, getColorAtX(box.maxX))
            putVertex(midX, box.maxY, box.maxZ, getColorAtX(midX))
            putVertex(midX, box.minY, box.maxZ, getColorAtX(midX))
        }
        if ((sides and 0x10) != 0) {
            val c = getColorAtX(box.minX)
            putVertex(box.minX, box.minY, box.maxZ, c)
            putVertex(box.minX, box.maxY, box.maxZ, c)
            putVertex(box.minX, box.maxY, box.minZ, c)
            putVertex(box.minX, box.minY, box.minZ, c)
        }
        if ((sides and 0x20) != 0) {
            val c = getColorAtX(box.maxX)
            putVertex(box.maxX, box.minY, box.minZ, c)
            putVertex(box.maxX, box.maxY, box.minZ, c)
            putVertex(box.maxX, box.maxY, box.maxZ, c)
            putVertex(box.maxX, box.minY, box.maxZ, c)
        }
    }

    fun drawTripleVerticalGradientOutline(box: AxisAlignedBB, colorTop: ColorRGB, colorMiddle: ColorRGB, colorBottom: ColorRGB, progress: Float) {
        val height = box.maxY - box.minY
        val midY = (box.minY + box.maxY) / 2.0
        val q1 = box.minY + height * 0.25
        val q3 = box.minY + height * 0.75

        fun getColorAtY(y: Double): ColorRGB {
            val relativePos = ((y - box.minY) / height).coerceIn(0.0, 1.0)
            return if (relativePos < 0.333) {
                ColorRGB.lerp(colorBottom, colorMiddle, (relativePos * 3).toFloat())
            } else if (relativePos < 0.667) {
                ColorRGB.lerp(colorMiddle, colorTop, ((relativePos - 0.333) * 3).toFloat())
            } else {
                ColorRGB.lerp(colorTop, colorBottom, ((relativePos - 0.667) * 3).toFloat())
            }
        }

        val topC = getColorAtY(box.maxY)
        putVertex(box.minX, box.maxY, box.minZ, topC)
        putVertex(box.maxX, box.maxY, box.minZ, topC)
        putVertex(box.maxX, box.maxY, box.minZ, topC)
        putVertex(box.maxX, box.maxY, box.maxZ, topC)
        putVertex(box.maxX, box.maxY, box.maxZ, topC)
        putVertex(box.minX, box.maxY, box.maxZ, topC)
        putVertex(box.minX, box.maxY, box.maxZ, topC)
        putVertex(box.minX, box.maxY, box.minZ, topC)

        val botC = getColorAtY(box.minY)
        putVertex(box.minX, box.minY, box.minZ, botC)
        putVertex(box.maxX, box.minY, box.minZ, botC)
        putVertex(box.maxX, box.minY, box.minZ, botC)
        putVertex(box.maxX, box.minY, box.maxZ, botC)
        putVertex(box.maxX, box.minY, box.maxZ, botC)
        putVertex(box.minX, box.minY, box.maxZ, botC)
        putVertex(box.minX, box.minY, box.maxZ, botC)
        putVertex(box.minX, box.minY, box.minZ, botC)

        fun drawVerticalLine(x: Double, z: Double) {
            putVertex(x, box.minY, z, getColorAtY(box.minY))
            putVertex(x, q1, z, getColorAtY(q1))
            putVertex(x, q1, z, getColorAtY(q1))
            putVertex(x, midY, z, getColorAtY(midY))
            putVertex(x, midY, z, getColorAtY(midY))
            putVertex(x, q3, z, getColorAtY(q3))
            putVertex(x, q3, z, getColorAtY(q3))
            putVertex(x, box.maxY, z, getColorAtY(box.maxY))
        }

        drawVerticalLine(box.minX, box.minZ)
        drawVerticalLine(box.maxX, box.minZ)
        drawVerticalLine(box.maxX, box.maxZ)
        drawVerticalLine(box.minX, box.maxZ)
    }

    fun drawTripleHorizontalGradientOutline(box: AxisAlignedBB, colorLeft: ColorRGB, colorMiddle: ColorRGB, colorRight: ColorRGB, progress: Float) {
        val width = box.maxX - box.minX
        val midX = (box.minX + box.maxX) / 2.0
        val q1 = box.minX + width * 0.25
        val q3 = box.minX + width * 0.75

        fun getColorAtX(x: Double): ColorRGB {
            val relativePos = ((x - box.minX) / width).coerceIn(0.0, 1.0)
            return if (relativePos < 0.333) {
                ColorRGB.lerp(colorLeft, colorMiddle, (relativePos * 3).toFloat())
            } else if (relativePos < 0.667) {
                ColorRGB.lerp(colorMiddle, colorRight, ((relativePos - 0.333) * 3).toFloat())
            } else {
                ColorRGB.lerp(colorRight, colorLeft, ((relativePos - 0.667) * 3).toFloat())
            }
        }

        fun drawHorizontalLineY(y: Double, z: Double) {
            putVertex(box.minX, y, z, getColorAtX(box.minX))
            putVertex(q1, y, z, getColorAtX(q1))
            putVertex(q1, y, z, getColorAtX(q1))
            putVertex(midX, y, z, getColorAtX(midX))
            putVertex(midX, y, z, getColorAtX(midX))
            putVertex(q3, y, z, getColorAtX(q3))
            putVertex(q3, y, z, getColorAtX(q3))
            putVertex(box.maxX, y, z, getColorAtX(box.maxX))
        }

        drawHorizontalLineY(box.minY, box.minZ)
        drawHorizontalLineY(box.maxY, box.minZ)
        drawHorizontalLineY(box.minY, box.maxZ)
        drawHorizontalLineY(box.maxY, box.maxZ)

        fun drawHorizontalLineX(x: Double, y: Double) {
            val c = getColorAtX(x)
            putVertex(x, y, box.minZ, c)
            putVertex(x, y, box.maxZ, c)
        }

        drawHorizontalLineX(box.minX, box.minY)
        drawHorizontalLineX(box.minX, box.maxY)
        drawHorizontalLineX(box.maxX, box.minY)
        drawHorizontalLineX(box.maxX, box.maxY)
    }

    fun drawVerticalGradientOutline(box: AxisAlignedBB, colorTop: ColorRGB, colorBottom: ColorRGB) {
        putVertex(box.minX, box.maxY, box.minZ, colorTop)
        putVertex(box.maxX, box.maxY, box.minZ, colorTop)
        putVertex(box.maxX, box.maxY, box.minZ, colorTop)
        putVertex(box.maxX, box.maxY, box.maxZ, colorTop)
        putVertex(box.maxX, box.maxY, box.maxZ, colorTop)
        putVertex(box.minX, box.maxY, box.maxZ, colorTop)
        putVertex(box.minX, box.maxY, box.maxZ, colorTop)
        putVertex(box.minX, box.maxY, box.minZ, colorTop)

        putVertex(box.minX, box.minY, box.minZ, colorBottom)
        putVertex(box.maxX, box.minY, box.minZ, colorBottom)
        putVertex(box.maxX, box.minY, box.minZ, colorBottom)
        putVertex(box.maxX, box.minY, box.maxZ, colorBottom)
        putVertex(box.maxX, box.minY, box.maxZ, colorBottom)
        putVertex(box.minX, box.minY, box.maxZ, colorBottom)
        putVertex(box.minX, box.minY, box.maxZ, colorBottom)
        putVertex(box.minX, box.minY, box.minZ, colorBottom)

        putVertex(box.minX, box.minY, box.minZ, colorBottom)
        putVertex(box.minX, box.maxY, box.minZ, colorTop)
        putVertex(box.maxX, box.minY, box.minZ, colorBottom)
        putVertex(box.maxX, box.maxY, box.minZ, colorTop)
        putVertex(box.maxX, box.minY, box.maxZ, colorBottom)
        putVertex(box.maxX, box.maxY, box.maxZ, colorTop)
        putVertex(box.minX, box.minY, box.maxZ, colorBottom)
        putVertex(box.minX, box.maxY, box.maxZ, colorTop)
    }

    fun drawHorizontalGradientOutline(box: AxisAlignedBB, colorLeft: ColorRGB, colorRight: ColorRGB) {
        putVertex(box.minX, box.minY, box.minZ, colorLeft)
        putVertex(box.maxX, box.minY, box.minZ, colorRight)
        putVertex(box.minX, box.maxY, box.minZ, colorLeft)
        putVertex(box.maxX, box.maxY, box.minZ, colorRight)
        putVertex(box.minX, box.minY, box.minZ, colorLeft)
        putVertex(box.minX, box.maxY, box.minZ, colorLeft)
        putVertex(box.maxX, box.minY, box.minZ, colorRight)
        putVertex(box.maxX, box.maxY, box.minZ, colorRight)

        putVertex(box.minX, box.minY, box.maxZ, colorLeft)
        putVertex(box.maxX, box.minY, box.maxZ, colorRight)
        putVertex(box.minX, box.maxY, box.maxZ, colorLeft)
        putVertex(box.maxX, box.maxY, box.maxZ, colorRight)
        putVertex(box.minX, box.minY, box.maxZ, colorLeft)
        putVertex(box.minX, box.maxY, box.maxZ, colorLeft)
        putVertex(box.maxX, box.minY, box.maxZ, colorRight)
        putVertex(box.maxX, box.maxY, box.maxZ, colorRight)

        putVertex(box.minX, box.minY, box.minZ, colorLeft)
        putVertex(box.minX, box.minY, box.maxZ, colorLeft)
        putVertex(box.maxX, box.minY, box.minZ, colorRight)
        putVertex(box.maxX, box.minY, box.maxZ, colorRight)
        putVertex(box.minX, box.maxY, box.minZ, colorLeft)
        putVertex(box.minX, box.maxY, box.maxZ, colorLeft)
        putVertex(box.maxX, box.maxY, box.minZ, colorRight)
        putVertex(box.maxX, box.maxY, box.maxZ, colorRight)
    }

    fun drawGradientLineTo(start: Vec3d, end: Vec3d, colorStart: ColorRGB, colorEnd: ColorRGB) {
        putVertex(camPos.x, camPos.y, camPos.z, colorStart)
        putVertex(end.x, end.y, end.z, colorEnd)
    }

    fun drawGradientLine(start: Vec3d, end: Vec3d, colorStart: ColorRGB, colorEnd: ColorRGB) {
        putVertex(start.x, start.y, start.z, colorStart)
        putVertex(end.x, end.y, end.z, colorEnd)
    }

    fun putVertex(posX: Double, posY: Double, posZ: Double, color: ColorRGB) {
        val array = PersistentMappedVBO.arr
        val struct = Pos3Color(array)
        struct.pos.x = (posX + translationX).toFloat()
        struct.pos.y = (posY + translationY).toFloat()
        struct.pos.z = (posZ + translationZ).toFloat()
        struct.color = color.rgba
        array.plusAssign(Pos3Color.size)
        vertexSize++
    }

    fun draw(mode: Int) {
        if (vertexSize == 0) return
        DrawShader.bind()
        GL30.glBindVertexArray(PersistentMappedVBO.POS3_COLOR)
        GL11.glDrawArrays(mode, PersistentMappedVBO.drawOffset, vertexSize)
        PersistentMappedVBO.end()
        GL30.glBindVertexArray(0)
        vertexSize = 0
    }

    fun prepareGL() {
        GlStateManager.pushMatrix()
        GlStateManager.glLineWidth(1.0f)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(34383) // GL_MULTISAMPLE
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GlStateManager.disableAlpha()
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
        GlStateManager.enableBlend()
        GlStateManager.depthMask(false)
        GlStateManager.disableTexture2D()
        GlStateManager.disableLighting()
    }

    fun releaseGL() {
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.shadeModel(GL11.GL_FLAT)
        GlStateManager.enableAlpha()
        GlStateManager.depthMask(true)
        GL11.glDisable(34383) // GL_MULTISAMPLE
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        GlStateManager.glLineWidth(1.0f)
        GlStateManager.popMatrix()
    }

    private object DrawShader : Shader("/assets/meta/shaders/general/Pos3Color.vsh", "/assets/meta/shaders/general/Pos3Color.fsh")
}