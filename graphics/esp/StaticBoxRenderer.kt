package dev.wizard.meta.graphics.esp

import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.MatrixUtils
import dev.wizard.meta.graphics.mask.BoxOutlineMask
import dev.wizard.meta.graphics.mask.BoxVertexMask
import dev.wizard.meta.graphics.mask.SideMask
import dev.wizard.meta.graphics.shaders.DrawShader
import dev.wizard.meta.util.accessor.renderPosX
import dev.wizard.meta.util.accessor.renderPosY
import dev.wizard.meta.util.accessor.renderPosZ
import dev.wizard.meta.util.threads.onMainThread
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class StaticBoxRenderer : AbstractIndexedRenderer() {
    private var filledSize: Int = 0
    private var outlineSize: Int = 0

    fun update(block: Builder.() -> Unit) {
        val builder = Builder()
        builder.block()
        builder.upload()
    }

    fun update(): Builder {
        return Builder()
    }

    fun render(filledAlpha: Int, outlineAlpha: Int) {
        if (size == 0 || !initialized) return
        val filled = filledAlpha > 0
        val outline = outlineAlpha > 0
        if (!filled && !outline) return

        Shader.bind()
        GL30.glBindVertexArray(vaoID)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboID)
        Shader.translate(posX, posY, posZ)

        if (filled) {
            Shader.alpha(filledAlpha)
            GL11.glDrawElements(GL11.GL_TRIANGLES, filledSize, GL11.GL_UNSIGNED_SHORT, 0L)
        }
        if (outline) {
            Shader.alpha(outlineAlpha)
            GL11.glDrawElements(GL11.GL_LINES, outlineSize, GL11.GL_UNSIGNED_SHORT, filledSize.toLong() * 2L)
        }

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
    }

    inner class Builder {
        private val posList = FloatArrayList()
        private val intList = IntArrayList()
        private val filledIndexList = ShortArrayList()
        private val outlineIndexList = ShortArrayList()
        private val renderPosX: Double
        private val renderPosY: Double
        private val renderPosZ: Double
        private var boxSizeInternal = 0
        private var vertexSizeInternal = 0

        init {
            val renderManager = mc.renderManager
            renderPosX = renderManager.renderPosX
            renderPosY = renderManager.renderPosY
            renderPosZ = renderManager.renderPosZ
        }

        fun putBox(box: AxisAlignedBB, color: Int, sideMask: Int) {
            putBox(box, color, sideMask, SideMask.toOutlineMask(sideMask))
        }

        fun putBox(box: AxisAlignedBB, color: Int, sideMask: Int, outlineMask: Int) {
            if ((sideMask or outlineMask) == 0) return
            if (sideMask == SideMask.ALL && outlineMask == BoxOutlineMask.ALL) {
                putBox(box, color)
                return
            }

            val minX = (box.minX - renderPosX).toFloat()
            val minY = (box.minY - renderPosY).toFloat()
            val minZ = (box.minZ - renderPosZ).toFloat()
            val maxX = (box.maxX - renderPosX).toFloat()
            val maxY = (box.maxY - renderPosY).toFloat()
            val maxZ = (box.maxZ - renderPosZ).toFloat()

            val vertexMask = SideMask.toVertexMask(sideMask) or BoxOutlineMask.toVertexMask(outlineMask)
            putFilledIndex(vertexMask, sideMask)
            putOutlineIndex(vertexMask, outlineMask)

            val prev = vertexSizeInternal
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XN_YN_ZN)) putVertex(minX, minY, minZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XN_YN_ZP)) putVertex(minX, minY, maxZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XN_YP_ZN)) putVertex(minX, maxY, minZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XN_YP_ZP)) putVertex(minX, maxY, maxZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XP_YN_ZN)) putVertex(maxX, minY, minZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XP_YN_ZP)) putVertex(maxX, minY, maxZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XP_YP_ZN)) putVertex(maxX, maxY, minZ)
            if (BoxVertexMask.contains(vertexMask, BoxVertexMask.XP_YP_ZP)) putVertex(maxX, maxY, maxZ)

            intList.add(color)
            intList.add(vertexSizeInternal - prev)
            boxSizeInternal++
        }

        fun putBox(box: AxisAlignedBB, color: Int) {
            val minX = (box.minX - renderPosX).toFloat()
            val minY = (box.minY - renderPosY).toFloat()
            val minZ = (box.minZ - renderPosZ).toFloat()
            val maxX = (box.maxX - renderPosX).toFloat()
            val maxY = (box.maxY - renderPosY).toFloat()
            val maxZ = (box.maxZ - renderPosZ).toFloat()

            putFilledIndexAll()
            putOutlineIndexAll()

            putVertex(minX, minY, minZ)
            putVertex(minX, minY, maxZ)
            putVertex(minX, maxY, minZ)
            putVertex(minX, maxY, maxZ)
            putVertex(maxX, minY, minZ)
            putVertex(maxX, minY, maxZ)
            putVertex(maxX, maxY, minZ)
            putVertex(maxX, maxY, maxZ)

            intList.add(color)
            intList.add(8)
            boxSizeInternal++
        }

        private fun putFilledIndexAll() {
            val o = vertexSizeInternal.toShort()
            filledIndexList.add(o); filledIndexList.add((o + 5).toShort()); filledIndexList.add((o + 1).toShort())
            filledIndexList.add(o); filledIndexList.add((o + 4).toShort()); filledIndexList.add((o + 5).toShort())
            filledIndexList.add((o + 3).toShort()); filledIndexList.add((o + 6).toShort()); filledIndexList.add((o + 2).toShort())
            filledIndexList.add((o + 3).toShort()); filledIndexList.add((o + 7).toShort()); filledIndexList.add((o + 6).toShort())
            filledIndexList.add((o + 2).toShort()); filledIndexList.add((o + 4).toShort()); filledIndexList.add(o)
            filledIndexList.add((o + 2).toShort()); filledIndexList.add((o + 6).toShort()); filledIndexList.add((o + 4).toShort())
            filledIndexList.add((o + 7).toShort()); filledIndexList.add((o + 1).toShort()); filledIndexList.add((o + 5).toShort())
            filledIndexList.add((o + 7).toShort()); filledIndexList.add((o + 3).toShort()); filledIndexList.add((o + 1).toShort())
            filledIndexList.add((o + 3).toShort()); filledIndexList.add(o); filledIndexList.add((o + 1).toShort())
            filledIndexList.add((o + 3).toShort()); filledIndexList.add((o + 2).toShort()); filledIndexList.add(o)
            filledIndexList.add((o + 6).toShort()); filledIndexList.add((o + 5).toShort()); filledIndexList.add((o + 4).toShort())
            filledIndexList.add((o + 6).toShort()); filledIndexList.add((o + 7).toShort()); filledIndexList.add((o + 5).toShort())
        }

        private fun putOutlineIndexAll() {
            val o = vertexSizeInternal.toShort()
            outlineIndexList.add(o); outlineIndexList.add((o + 1).toShort())
            outlineIndexList.add((o + 1).toShort()); outlineIndexList.add((o + 5).toShort())
            outlineIndexList.add((o + 5).toShort()); outlineIndexList.add((o + 4).toShort())
            outlineIndexList.add((o + 4).toShort()); outlineIndexList.add(o)
            outlineIndexList.add((o + 2).toShort()); outlineIndexList.add((o + 3).toShort())
            outlineIndexList.add((o + 3).toShort()); outlineIndexList.add((o + 7).toShort())
            outlineIndexList.add((o + 7).toShort()); outlineIndexList.add((o + 6).toShort())
            outlineIndexList.add((o + 6).toShort()); outlineIndexList.add((o + 2).toShort())
            outlineIndexList.add(o); outlineIndexList.add((o + 2).toShort())
            outlineIndexList.add((o + 1).toShort()); outlineIndexList.add((o + 3).toShort())
            outlineIndexList.add((o + 4).toShort()); outlineIndexList.add((o + 6).toShort())
            outlineIndexList.add((o + 5).toShort()); outlineIndexList.add((o + 7).toShort())
        }

        private fun putFilledIndex(vertexMask: Int, sideMask: Int) {
            val o = vertexSizeInternal.toShort()
            if (SideMask.contains(sideMask, SideMask.DOWN)) {
                filledIndexList.add(o)
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort())
                filledIndexList.add(o)
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
            }
            if (SideMask.contains(sideMask, SideMask.UP)) {
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
            }
            if (SideMask.contains(sideMask, SideMask.NORTH)) {
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort())
                filledIndexList.add(o)
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort())
            }
            if (SideMask.contains(sideMask, SideMask.SOUTH)) {
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort())
            }
            if (SideMask.contains(sideMask, SideMask.WEST)) {
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
                filledIndexList.add(o)
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort())
                filledIndexList.add(o)
            }
            if (SideMask.contains(sideMask, SideMask.EAST)) {
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
                filledIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
            }
        }

        private fun putOutlineIndex(vertexMask: Int, outlineMask: Int) {
            val o = vertexSizeInternal.toShort()
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.DOWN_NORTH)) {
                outlineIndexList.add(o); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.DOWN_SOUTH)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.DOWN_EAST)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.DOWN_WEST)) {
                outlineIndexList.add(o); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.UP_NORTH)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.UP_SOUTH)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.UP_WEST)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.UP_EAST)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.NORTH_WEST)) {
                outlineIndexList.add(o); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 2)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.NORTH_EAST)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 4)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 6)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.SOUTH_WEST)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 1)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 3)).toShort())
            }
            if (BoxOutlineMask.contains(outlineMask, BoxOutlineMask.SOUTH_EAST)) {
                outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 5)).toShort()); outlineIndexList.add((o + BoxVertexMask.countBits(vertexMask, 7)).toShort())
            }
        }

        private fun putVertex(x: Float, y: Float, z: Float) {
            posList.add(x); posList.add(y); posList.add(z)
            vertexSizeInternal++
        }

        fun upload() {
            val vboBuffer = buildVboBuffer()
            val iboBuffer = buildIboBuffer()
            val fSize = filledIndexList.size
            val oSize = outlineIndexList.size

            onMainThread {
                if (vaoID == 0) vaoID = GL30.glGenVertexArrays()
                if (initialized) {
                    GL15.glDeleteBuffers(vboID)
                    GL15.glDeleteBuffers(iboID)
                }
                vboID = GL15.glGenBuffers()
                iboID = GL15.glGenBuffers()

                GL30.glBindVertexArray(vaoID)
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboBuffer, GL15.GL_STATIC_DRAW)

                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 16, 0L)
                GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, 16, 12L)

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
                GL20.glEnableVertexAttribArray(0)
                GL20.glEnableVertexAttribArray(1)
                GL30.glBindVertexArray(0)

                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboID)
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, iboBuffer, GL15.GL_STATIC_DRAW)
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)

                posX = renderPosX
                posY = renderPosY
                posZ = renderPosZ
                size = boxSizeInternal
                filledSize = fSize
                outlineSize = oSize
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val buffer = GLAllocation.createDirectByteBuffer(vertexSizeInternal * 16)
            var vertexIndex = 0
            for (colorIndex in 0 until intList.size step 2) {
                val color = intList.getInt(colorIndex)
                val count = intList.getInt(colorIndex + 1)
                for (i in 0 until count) {
                    buffer.putFloat(posList.getFloat(vertexIndex++))
                    buffer.putFloat(posList.getFloat(vertexIndex++))
                    buffer.putFloat(posList.getFloat(vertexIndex++))
                    buffer.putInt(color)
                }
            }
            buffer.flip()
            return buffer
        }

        private fun buildIboBuffer(): ShortBuffer {
            val buffer = GLAllocation.createDirectByteBuffer((filledIndexList.size + outlineIndexList.size) * 2).asShortBuffer()
            buffer.put(filledIndexList.toShortArray())
            buffer.put(outlineIndexList.toShortArray())
            buffer.flip()
            return buffer
        }
    }

    object Shader : DrawShader("/assets/meta/shaders/general/StaticBoxRenderer.vsh", "/assets/meta/shaders/general/Renderer.fsh"), AlwaysListening {
        private val alphaUniform = GL20.glGetUniformLocation(id, "alpha")

        init {
            listener<Render3DEvent>(priority = 0x7FFFFFFE, alwaysListening = true) {
                bind()
                updateProjectionMatrix()
            }
        }

        fun translate(xOffset: Double, yOffset: Double, zOffset: Double) {
            val renderManager = mc.renderManager
            val x = (xOffset - renderManager.renderPosX).toFloat()
            val y = (yOffset - renderManager.renderPosY).toFloat()
            val z = (zOffset - renderManager.renderPosZ).toFloat()
            val modelView = MatrixUtils.loadModelViewMatrix().getMatrix().translate(x, y, z)
            updateModelViewMatrix(modelView)
        }

        fun alpha(value: Int) {
            GL20.glUniform1f(alphaUniform, value / 255.0f)
        }
    }
}
