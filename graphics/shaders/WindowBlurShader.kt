package dev.wizard.meta.graphics.shaders

import dev.luna5ama.kmogus.MutableArr
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.events.render.ResolutionUpdateEvent
import dev.wizard.meta.graphics.GLDataType
import dev.wizard.meta.graphics.GLObjectKt.use
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.MatrixUtils
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.VertexAttribute
import dev.wizard.meta.graphics.buffer.PersistentMappedVBO
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.structs.Vec4f32
import dev.wizard.meta.util.interfaces.Helper
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL41
import org.lwjgl.opengl.GL45

object WindowBlurShader : AlwaysListening, Helper {
    private val vao: Int
    private val fbo1: Framebuffer
    private val fbo2: Framebuffer
    private val passH: Pass
    private val passV: Pass

    init {
        val attribute = VertexAttribute.Builder(16).apply {
            float(0, 4, GLDataType.GL_FLOAT, false, 0)
        }.build()
        vao = PersistentMappedVBO.createVao(attribute)

        fbo1 = Framebuffer(mc.field_71443_c, mc.field_71440_d, false)
        fbo2 = Framebuffer(mc.field_71443_c, mc.field_71440_d, false)

        passH = Pass("/assets/meta/shaders/gui/WindowBlurH.vsh")
        passV = Pass("/assets/meta/shaders/gui/WindowBlurV.vsh")

        updateResolution(mc.field_71443_c, mc.field_71440_d)

        listener<ResolutionUpdateEvent>(alwaysListening = true) {
            updateResolution(it.width, it.height)
        }
    }

    private fun updateResolution(width: Int, height: Int) {
        passH.updateResolution(width.toFloat(), height.toFloat())
        passV.updateResolution(width.toFloat(), height.toFloat())
        fbo1.func_147613_a(width, height)
        setTextureParam(fbo1.field_147617_g)
        fbo2.func_147613_a(width, height)
        setTextureParam(fbo2.field_147617_g)
    }

    fun render(x: Float, y: Float) {
        render(0.0f, 0.0f, x, y)
    }

    fun renderCapsule(x: Float, y: Float, x2: Float, y2: Float, segments: Int = 50, left: Boolean = false, right: Boolean = false) {
        GL11.glEnable(GL11.GL_STENCIL_TEST)
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF)
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)
        GL11.glColorMask(false, false, false, false)

        val white = ColorRGB(255, 255, 255)
        when {
            left -> RenderUtils2D.drawLeftCapsuleRectFilled(x, y, x2, y2, white, segments)
            right -> RenderUtils2D.drawRightCapsuleRectFilled(x, y, x2, y2, white, segments)
            else -> RenderUtils2D.drawCapsuleRectFilled(x, y, x2, y2, white, segments)
        }

        GL11.glColorMask(true, true, true, true)
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF)
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
        render(x, y, x2, y2)
        GL11.glDisable(GL11.GL_STENCIL_TEST)
    }

    fun render(x1: Float, y1: Float, x2: Float, y2: Float) {
        val pass = ClickGUI.INSTANCE.windowBlurPass
        if (pass == 0) return

        setTextureParam(mc.func_147110_a().field_147617_g)
        putVertices(x1, y1, x2, y2)

        GlStateManager.func_179098_w()
        GlStateManager.func_179138_g(OpenGlHelper.field_77478_a)
        GlStateManager.func_179097_i()
        GlStateManager.func_179132_a(false)
        GlStateUtils.blend(false)

        passH.updateMatrix()
        passV.updateMatrix()

        GL30.glBindVertexArray(vao)
        var extend = pass.toFloat() - 1.0f
        val mcFbo = mc.func_147110_a()

        bindFbo(mcFbo, fbo1)
        drawPass(passH, extend, extend + 1.0f)

        while (extend > 0.0f) {
            bindFbo(fbo1, fbo2)
            drawPass(passV, extend, extend)
            bindFbo(fbo2, fbo1)
            drawPass(passH, extend - 1.0f, extend)
            extend -= 1.0f
        }

        bindFbo(fbo1, mcFbo)
        drawPass(passV, 0.0f, 0.0f)

        fbo1.func_147606_d()
        GlStateUtils.blend(true)
        PersistentMappedVBO.end()
        GL30.glBindVertexArray(0)
    }

    private fun drawPass(shader: Pass, x: Float, y: Float) {
        shader.bind()
        shader.updateExtend(x, y)
        GL11.glDrawArrays(GL11.GL_TRIANGLES, PersistentMappedVBO.drawOffset, 6)
    }

    private fun bindFbo(from: Framebuffer, to: Framebuffer) {
        from.func_147612_c()
        to.func_147610_a(false)
    }

    private fun putVertices(x1: Float, y1: Float, x2: Float, y2: Float) {
        val array = PersistentMappedVBO.arr
        var struct = Vec4f32(array)

        fun put(x: Float, y: Float, z: Float, w: Float) {
            struct.x = x
            struct.y = y
            struct.z = z
            struct.w = w
            struct = struct.inc()
        }

        put(x1, y1, -1.0f, 1.0f)
        put(x1, y2, -1.0f, -1.0f)
        put(x2, y2, 1.0f, -1.0f)
        put(x2, y1, 1.0f, 1.0f)
        put(x1, y1, -1.0f, 1.0f)
        put(x2, y2, 1.0f, -1.0f)

        array.pos = struct.ptr
    }

    private fun setTextureParam(textureID: Int) {
        GL45.glTextureParameteri(textureID, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL45.glTextureParameteri(textureID, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL45.glTextureParameteri(textureID, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE)
        GL45.glTextureParameteri(textureID, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE)
    }

    private class Pass(vertShaderPath: String) : DrawShader(vertShaderPath, "/assets/meta/shaders/gui/WindowBlur.fsh") {
        private val reverseProjectionUniform: Int = GL20.glGetUniformLocation(id, "reverseProjection")
        private val resolutionUniform: Int = GL20.glGetUniformLocation(id, "resolution")
        private val extendUniform: Int = GL20.glGetUniformLocation(id, "extend")

        init {
            use {
                updateResolution(mc.field_71443_c.toFloat(), mc.field_71440_d.toFloat())
                GL20.glUniform1i(GL20.glGetUniformLocation(id, "background"), 0)
            }
        }

        fun updateExtend(x: Float, y: Float) {
            GL41.glProgramUniform2f(id, extendUniform, x, y)
        }

        fun updateResolution(width: Float, height: Float) {
            GL41.glProgramUniform2f(id, resolutionUniform, width, height)
            val matrix = Matrix4f().ortho(0.0f, width, 0.0f, height, 1000.0f, 3000.0f).invert()
            MatrixUtils.loadMatrix(matrix).uploadMatrix(id, reverseProjectionUniform)
        }
    }
}
