package dev.wizard.meta.graphics

import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.OpenGlHelper
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL41
import java.nio.FloatBuffer

object MatrixUtils {
    val matrixBuffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)

    fun loadProjectionMatrix(): MatrixUtils {
        matrixBuffer.clear()
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer)
        return this
    }

    fun loadModelViewMatrix(): MatrixUtils {
        matrixBuffer.clear()
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer)
        return this
    }

    fun loadMatrix(matrix: Matrix4f): MatrixUtils {
        matrix.get(matrixBuffer)
        return this
    }

    fun getMatrix(): Matrix4f {
        return Matrix4f(matrixBuffer)
    }

    fun getMatrix(matrix: Matrix4f): Matrix4f {
        matrix.set(matrixBuffer)
        return matrix
    }

    fun uploadMatrix(location: Int) {
        OpenGlHelper.glUniformMatrix4(location, false, matrixBuffer)
    }

    fun uploadMatrix(id: Int, location: Int) {
        GL41.glProgramUniformMatrix4fv(id, location, false, matrixBuffer)
    }
}