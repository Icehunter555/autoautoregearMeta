package dev.wizard.meta.graphics.shaders

import dev.wizard.meta.graphics.MatrixUtils
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL41
import java.nio.FloatBuffer

open class DrawShader(vertShaderPath: String, fragShaderPath: String) : Shader(vertShaderPath, fragShaderPath) {
    private val projectionUniform: Int = GL20.glGetUniformLocation(id, "projection")
    private val modelViewUniform: Int = GL20.glGetUniformLocation(id, "modelView")

    fun updateMatrix() {
        updateModelViewMatrix()
        updateProjectionMatrix()
    }

    fun updateProjectionMatrix() {
        MatrixUtils.loadProjectionMatrix().uploadMatrix(id, projectionUniform)
    }

    fun updateProjectionMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(id, projectionUniform)
    }

    fun uploadProjectionMatrix(buffer: FloatBuffer) {
        GL41.glProgramUniformMatrix4(id, projectionUniform, false, buffer)
    }

    fun updateModelViewMatrix() {
        MatrixUtils.loadModelViewMatrix().uploadMatrix(id, modelViewUniform)
    }

    fun updateModelViewMatrix(matrix: Matrix4f) {
        MatrixUtils.loadMatrix(matrix).uploadMatrix(id, modelViewUniform)
    }

    fun uploadModelViewMatrix(buffer: FloatBuffer) {
        GL41.glProgramUniformMatrix4(id, modelViewUniform, false, buffer)
    }
}
