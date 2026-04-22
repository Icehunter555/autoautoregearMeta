package dev.wizard.meta.graphics.esp

import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.MatrixUtils
import dev.wizard.meta.graphics.shaders.DrawShader
import dev.wizard.meta.util.accessor.renderPosX
import dev.wizard.meta.util.accessor.renderPosY
import dev.wizard.meta.util.accessor.renderPosZ
import dev.wizard.meta.util.math.xCenter
import dev.wizard.meta.util.math.yCenter
import dev.wizard.meta.util.math.zCenter
import dev.wizard.meta.util.threads.onMainThread
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer

class StaticTracerRenderer : AbstractRenderer() {
    fun render(tracerAlpha: Int) {
        if (size == 0 || !initialized) return
        if (tracerAlpha > 0) {
            Shader.bind()
            GL30.glBindVertexArray(vaoID)
            Shader.translate(posX, posY, posZ)
            Shader.alpha(tracerAlpha)
            GL11.glDrawArrays(GL11.GL_LINES, 0, size * 2)
            GL30.glBindVertexArray(0)
        }
    }

    fun update(block: Builder.() -> Unit) {
        val builder = Builder()
        builder.block()
        builder.upload()
    }

    inner class Builder {
        private val posList = FloatArrayList()
        private val colorList = IntArrayList()
        private val renderPosX: Double
        private val renderPosY: Double
        private val renderPosZ: Double
        private var sizeInternal = 0

        init {
            val renderManager = mc.renderManager
            renderPosX = renderManager.renderPosX
            renderPosY = renderManager.renderPosY
            renderPosZ = renderManager.renderPosZ
        }

        fun putTracer(box: AxisAlignedBB, color: Int) {
            putTracer(box.xCenter, box.yCenter, box.zCenter, color)
        }

        fun putTracer(x: Double, y: Double, z: Double, color: Int) {
            posList.add((x - renderPosX).toFloat())
            posList.add((y - renderPosY).toFloat())
            posList.add((z - renderPosZ).toFloat())
            colorList.add(color)
            sizeInternal++
        }

        fun upload() {
            val vboBuffer = buildVboBuffer()
            onMainThread {
                if (vaoID == 0) vaoID = GL30.glGenVertexArrays()
                if (initialized) GL15.glDeleteBuffers(vboID)
                vboID = GL15.glGenBuffers()

                GL30.glBindVertexArray(vaoID)
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboBuffer, GL15.GL_STATIC_DRAW)

                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 16, 0L)
                GL20.glVertexAttribPointer(1, 4, GL11.GL_UNSIGNED_BYTE, true, 16, 12L)

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
                GL20.glEnableVertexAttribArray(0)
                GL20.glEnableVertexAttribArray(1)
                GL30.glBindVertexArray(0)

                posX = renderPosX
                posY = renderPosY
                posZ = renderPosZ
                size = sizeInternal
            }
        }

        private fun buildVboBuffer(): ByteBuffer {
            val buffer = GLAllocation.createDirectByteBuffer(sizeInternal * 2 * 16)
            var vertexIndex = 0
            for (i in 0 until colorList.size) {
                val color = colorList.getInt(i)
                buffer.position(buffer.position() + 12)
                buffer.putInt(color)
                buffer.putFloat(posList.getFloat(vertexIndex++))
                buffer.putFloat(posList.getFloat(vertexIndex++))
                buffer.putFloat(posList.getFloat(vertexIndex++))
                buffer.putInt(color)
            }
            buffer.flip()
            return buffer
        }
    }

    object Shader : DrawShader("/assets/meta/shaders/general/StaticTracerRenderer.vsh", "/assets/meta/shaders/general/Renderer.fsh"), AlwaysListening {
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
