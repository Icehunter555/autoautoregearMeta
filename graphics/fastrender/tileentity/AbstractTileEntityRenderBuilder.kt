package dev.wizard.meta.graphics.fastrender.tileentity

import dev.wizard.meta.graphics.GLObject
import dev.wizard.meta.graphics.MatrixUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.shaders.DrawShader
import dev.wizard.meta.graphics.use
import dev.wizard.meta.util.threads.onMainThread
import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import net.minecraft.tileentity.TileEntity
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL31
import java.nio.ByteBuffer

abstract class AbstractTileEntityRenderBuilder<T : TileEntity>(
    protected val builtPosX: Double,
    protected val builtPosY: Double,
    protected val builtPosZ: Double
) : ITileEntityRenderBuilder<T> {
    var size: Int = 0
        protected set

    protected val floatArrayList by lazy { FloatArrayList() }
    protected val shortArrayList by lazy { ShortArrayList() }
    protected val byteArrayList by lazy { ByteArrayList() }
    private var buffer: ByteBuffer? = null

    override fun build() {
        buffer = buildBuffer()
    }

    override fun upload(): ITileEntityRenderBuilder.Renderer {
        val b = buffer ?: buildBuffer()
        return uploadBuffer(b)
    }

    protected abstract fun buildBuffer(): ByteBuffer
    protected abstract fun uploadBuffer(vboBuffer: ByteBuffer): ITileEntityRenderBuilder.Renderer

    protected fun getTileEntityBlockMetadata(tileEntity: T): Int {
        return if (tileEntity.hasWorld()) tileEntity.blockMetadata else 0
    }

    protected fun putTileEntityLightMapUV(tileEntity: T) {
        val world = tileEntity.world
        if (world != null) {
            val i = world.getCombinedLight(tileEntity.pos, 0)
            byteArrayList.add((i and 0xFF).toByte())
            byteArrayList.add((i shr 16 and 0xFF).toByte())
        } else {
            byteArrayList.add(0.toByte())
            byteArrayList.add((-16).toByte())
        }
    }

    open class Renderer(
        private val shader: Shader,
        private val vaoID: Int,
        private val vboID: Int,
        private val modelSize: Int,
        private val size: Int,
        private val builtPosX: Double,
        private val builtPosY: Double,
        private val builtPosZ: Double
    ) : ITileEntityRenderBuilder.Renderer {
        override fun render(renderPosX: Double, renderPosY: Double, renderPosZ: Double) {
            shader.use {
                preRender()
                val x = builtPosX - renderPosX
                val y = builtPosY - renderPosY
                val z = builtPosZ - renderPosZ
                val modelView = MatrixUtils.loadModelViewMatrix().getMatrix().translate(x.toFloat(), y.toFloat(), z.toFloat())
                updateModelViewMatrix(modelView)
                GL30.glBindVertexArray(vaoID)
                GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, modelSize, size)
                GL30.glBindVertexArray(0)
                postRender()
            }
        }

        protected open fun preRender() {}
        protected open fun postRender() {}

        override fun destroy() {
            onMainThread {
                GL30.glDeleteVertexArrays(vaoID)
                GL15.glDeleteBuffers(vboID)
            }
        }
    }

    open class Shader(vertShaderPath: String, fragShaderPath: String) : DrawShader(vertShaderPath, fragShaderPath) {
        private val partialTicksUniform: Int = GL20.glGetUniformLocation(id, "partialTicks")

        init {
            use {
                GL20.glUniform1i(GL20.glGetUniformLocation(id, "lightMapTexture"), 1)
            }
            shaders.add(this)
        }

        protected open fun update() {
            uploadProjectionMatrix(MatrixUtils.matrixBuffer)
            GL20.glUniform1f(partialTicksUniform, RenderUtils3D.partialTicks)
        }

        companion object {
            private val shaders = ArrayList<Shader>()

            @JvmStatic
            fun updateShaders() {
                MatrixUtils.loadProjectionMatrix()
                shaders.forEach { shader ->
                    shader.use {
                        shader.update()
                    }
                }
            }
        }
    }
}
