package dev.wizard.meta.graphics.shaders

import dev.wizard.meta.graphics.GLObjectKt.use
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL20

open class GLSLSandbox(fragShaderPath: String) : Shader("/assets/meta/shaders/menu/DefaultVertex.vert.glsl", fragShaderPath) {
    private val timeUniform: Int = GL20.glGetUniformLocation(id, "time")
    private val mouseUniform: Int = GL20.glGetUniformLocation(id, "mouse")
    private val resolutionUniform: Int = GL20.glGetUniformLocation(id, "resolution")

    fun render(width: Float, height: Float, mouseX: Float, mouseY: Float, initTime: Long) {
        use {
            GL20.glUniform2f(resolutionUniform, width, height)
            GL20.glUniform2f(mouseUniform, mouseX / width, (height - 1.0f - mouseY) / height)
            GL20.glUniform1f(timeUniform, (System.currentTimeMillis() - initTime).toFloat() / 1000.0f)

            val tessellator = Tessellator.func_178181_a()
            val buffer = tessellator.func_178180_c()
            buffer.func_181668_a(7, DefaultVertexFormats.field_181705_e)
            buffer.func_181662_b(-1.0, -1.0, 0.2).func_181675_d()
            buffer.func_181662_b(1.0, -1.0, 0.2).func_181675_d()
            buffer.func_181662_b(1.0, 1.0, 0.2).func_181675_d()
            buffer.func_181662_b(-1.0, 1.0, 0.2).func_181675_d()
            tessellator.func_78381_a()
        }
    }
}
