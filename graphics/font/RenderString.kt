package dev.wizard.meta.graphics.font

import dev.luna5ama.kmogus.*
import dev.wizard.meta.graphics.GLDataType
import dev.wizard.meta.graphics.GLFunctionsKt
import dev.wizard.meta.graphics.GLObject
import dev.wizard.meta.graphics.GLObjectKt.use
import dev.wizard.meta.graphics.VertexAttribute
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.glyph.CharInfo
import dev.wizard.meta.graphics.font.glyph.GlyphChunk
import dev.wizard.meta.graphics.font.renderer.AbstractFontRenderContext
import dev.wizard.meta.graphics.font.renderer.AbstractFontRenderer
import dev.wizard.meta.graphics.shaders.DrawShader
import dev.wizard.meta.module.modules.misc.AltProtect
import dev.wizard.meta.structs.FontVertex
import dev.wizard.meta.structs.FontVertexKt.sizeof
import dev.wizard.meta.structs.Vec2f32
import dev.wizard.meta.structs.Vec2i16
import dev.wizard.meta.util.collections.forEachFast
import net.minecraft.client.renderer.GlStateManager
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL45
import java.util.*

class RenderString(private val string: CharSequence, fontRenderer: AbstractFontRenderer) {
    private val renderInfoList = ArrayList<StringRenderInfo>()
    private val initTime = System.currentTimeMillis()
    private var lastAccess = initTime
    val width: Float
    var invalid: Boolean = false
        private set

    init {
        var finalString = string
        var maxLineWidth = 0.0f
        var width2 = 0.0f
        val context = fontRenderer.getRenderContext()

        for (i in string.indices) {
            val c = string[i]
            if (c == '\n') {
                if (width2 > maxLineWidth) {
                    maxLineWidth = width2
                }
                width2 = 0.0f
            }
            if (context.checkFormatCode(string, i, false)) continue
            width2 += fontRenderer.regularGlyph.getCharInfo(c).width + fontRenderer.charGap
        }

        if (string == AltProtect.getCurrentName()) {
            finalString = AltProtect.INSTANCE.fakeName.value
        }
        this.width = width2
    }

    fun build(fontRenderer: AbstractFontRenderer, charGap: Float, lineSpace: Float, shadowDist: Float): RenderString {
        val builders = Array(3) { arrayOfNulls<StringRenderInfo.Builder>(128) }
        var posX = 0.0f
        var posY = 0.0f
        val context = fontRenderer.getRenderContext()

        for (i in string.indices) {
            val c = string[i]
            if (context.checkFormatCode(string, i, true)) continue
            if (c == '\n') {
                posY += context.variant.fontHeight * lineSpace
                posX = 0.0f
                continue
            }

            val chunkID = c.code shr 9
            val chunk = context.variant.getChunk(chunkID)
            invalid = invalid || chunk.id != chunkID

            val charInfo = chunk.charInfoArray[c.code - (c.code shr 9 shl 9)]
            val variantArray = builders[context.variant.id]
            var builder = variantArray[chunk.id]
            if (builder == null) {
                builder = StringRenderInfo.Builder(chunk, shadowDist)
                variantArray[chunk.id] = builder
            }

            builder.put(posX, posY, charInfo, context)
            posX += charInfo.width + charGap
        }

        builders.forEach { array ->
            array.forEach { it?.let { renderInfoList.add(it.build()) } }
        }

        return this
    }

    fun render(modelView: Matrix4f, color: ColorRGB, drawShadow: Boolean, lodBias: Float) {
        lastAccess = System.currentTimeMillis()
        Shader.bind()
        Shader.preRender(modelView, color)
        renderInfoList.forEachFast { it.render(drawShadow, lodBias) }
        GL30.glBindVertexArray(0)
        GlStateManager.func_179144_i(0)
    }

    fun tryClean(current: Long): Boolean {
        return if (invalid || current - initTime >= 15000L || current - lastAccess >= 5000L) {
            destroy()
            true
        } else {
            false
        }
    }

    fun destroy() {
        renderInfoList.forEachFast { it.destroy() }
        renderInfoList.clear()
    }

    override fun equals(other: Any?):
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RenderString
        return string == other.string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }

    private object Shader : DrawShader("/assets/meta/shaders/general/FontRenderer.vsh", "/assets/meta/shaders/general/FontRenderer.fsh") {
        val defaultColorUniform = GL20.glGetUniformLocation(id, "defaultColor")

        init {
            use {
                GL20.glUniform1i(GL20.glGetUniformLocation(id, "texture"), 0)
            }
        }

        fun preRender(modelView: Matrix4f, color: ColorRGB) {
            updateProjectionMatrix()
            updateModelViewMatrix(modelView)
            GL20.glUniform4f(defaultColorUniform, color.rFloat, color.gFloat, color.bFloat, color.aFloat)
        }
    }

    private class StringRenderInfo(
        private val glyphChunk: GlyphChunk,
        private val size: Int,
        private val vaoID: Int,
        private val vboID: Int,
        private val iboID: Int
    ) {
        fun render(drawShadow: Boolean, lodBias: Float) {
            glyphChunk.texture.bindTexture()
            glyphChunk.updateLodBias(lodBias)
            GL30.glBindVertexArray(vaoID)
            if (drawShadow) {
                GLFunctionsKt.glDrawElements(4, size * 2 * 6, 5123, 0L)
            } else {
                GLFunctionsKt.glDrawElements(4, size * 6, 5123, size.toLong() * 6L * 2L)
            }
        }

        fun destroy() {
            GL30.glDeleteVertexArrays(vaoID)
            GL15.glDeleteBuffers(vboID)
            GL15.glDeleteBuffers(iboID)
        }

        class Builder(private val glyphChunk: GlyphChunk, private val shadowDist: Float) {
            private var size = 0
            private val array = Arr.malloc(128L).asMutable()

            companion object {
                val vertexAttribute = VertexAttribute.Builder(sizeof(FontVertex)).apply {
                    float(0, 2, GLDataType.GL_FLOAT, false, 0)
                    float(1, 2, GLDataType.GL_UNSIGNED_SHORT, true, 0)
                    int(2, 1, GLDataType.GL_BYTE, 0)
                    float(3, 1, GLDataType.GL_UNSIGNED_BYTE, false, 0)
                    float(4, 1, GLDataType.GL_UNSIGNED_BYTE, false, 0)
                }.build()
            }

            fun put(posX: Float, posY: Float, charInfo: CharInfo, context: AbstractFontRenderContext) {
                val color = (context.color + 1).toByte()
                val overrideColor = if (color == 0.toByte()) 1.toByte() else 0.toByte()
                var pX = posX
                var pY = posY
                val u = charInfo.uv[0]
                val v = charInfo.uv[1]
                size++
                array.ensureCapacity(size.toLong() * 16L * 4L * 2L, false)
                var struct = FontVertex(array)

                fun putVertex(x: Float, y: Float, u: Short, v: Short, shadow: Byte) {
                    struct.position.set(x, y)
                    struct.vertUV.set(u, v)
                    struct.colorIndex = color
                    struct.overrideColor = overrideColor
                    struct.shadow = shadow
                    struct = struct.inc()
                }

                putVertex(pX + shadowDist, pY + shadowDist, u, v, 1)
                putVertex(pX, pY, u, v, 0)

                pX = posX + charInfo.renderWidth
                pY = posY
                val u2 = charInfo.uv[2]
                val v2 = charInfo.uv[1]
                putVertex(pX + shadowDist, pY + shadowDist, u2, v2, 1)
                putVertex(pX, pY, u2, v2, 0)

                pX = posX
                pY = posY + charInfo.height
                val u3 = charInfo.uv[0]
                val v3 = charInfo.uv[3]
                putVertex(pX + shadowDist, pY + shadowDist, u3, v3, 1)
                putVertex(pX, pY, u3, v3, 0)

                pX = posX + charInfo.renderWidth
                pY = posY + charInfo.height
                val u4 = charInfo.uv[2]
                val v4 = charInfo.uv[3]
                putVertex(pX + shadowDist, pY + shadowDist, u4, v4, 1)
                putVertex(pX, pY, u4, v4, 0)

                array.pos = struct.ptr
            }

            fun build(): StringRenderInfo {
                val vaoID = GL45.glCreateVertexArrays()
                val vboID = GL45.glCreateBuffers()
                val iboID = GL45.glCreateBuffers()
                GLFunctionsKt.glNamedBufferStorage(vboID, size.toLong() * 16L * 4L * 2L, array.basePtr, 0)
                array.reset()
                buildIboBuffer()
                GLFunctionsKt.glNamedBufferStorage(iboID, size.toLong() * 2L * 6L * 2L, array.basePtr, 0)
                array.free()
                GL30.glBindVertexArray(vaoID)
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID)
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboID)
                vertexAttribute.apply()
                GL30.glBindVertexArray(0)
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
                return StringRenderInfo(glyphChunk, size, vaoID, vboID, iboID)
            }

            private fun buildIboBuffer() {
                val indexSize = size * 2 * 4
                var pointer = array.ptr
                for (index in 0 until indexSize step 8) {
                    Ptr.setShort(pointer, index.toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 4).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 2).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 6).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 2).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 4).toShort())
                    pointer += 2L
                }
                for (index in 0 until indexSize step 8) {
                    Ptr.setShort(pointer, (index + 1).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 5).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 3).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 7).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 3).toShort())
                    pointer += 2L
                    Ptr.setShort(pointer, (index + 5).toShort())
                    pointer += 2L
                }
            }
        }
    }
}
