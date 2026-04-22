package dev.wizard.meta.graphics.shaders

import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.GLObject
import dev.wizard.meta.util.IOUtilsKt
import dev.wizard.meta.util.interfaces.Helper
import org.lwjgl.opengl.GL20

open class Shader(vertShaderPath: String, fragShaderPath: String) : GLObject, Helper {
    override val id: Int

    init {
        val vertexShaderID = createShader(vertShaderPath, GL20.GL_VERTEX_SHADER)
        val fragShaderID = createShader(fragShaderPath, GL20.GL_FRAGMENT_SHADER)
        val programID = GL20.glCreateProgram()
        GL20.glAttachShader(programID, vertexShaderID)
        GL20.glAttachShader(programID, fragShaderID)
        GL20.glLinkProgram(programID)
        val linked = GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS)
        if (linked == 0) {
            MetaMod.logger.error(GL20.glGetProgramInfoLog(programID, 1024))
            GL20.glDeleteProgram(programID)
            throw IllegalStateException("Shader failed to link")
        }
        id = programID
        GL20.glDetachShader(programID, vertexShaderID)
        GL20.glDetachShader(programID, fragShaderID)
        GL20.glDeleteShader(vertexShaderID)
        GL20.glDeleteShader(fragShaderID)
    }

    private fun createShader(path: String, shaderType: Int): Int {
        val stream = this::class.java.getResourceAsStream(path) ?: throw IllegalStateException("Shader path not found: $path")
        val srcString = stream.use { IOUtilsKt.readText(it) }
        val shaderID = GL20.glCreateShader(shaderType)
        GL20.glShaderSource(shaderID, srcString)
        GL20.glCompileShader(shaderID)
        val compiled = GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS)
        if (compiled == 0) {
            MetaMod.logger.error(GL20.glGetShaderInfoLog(shaderID, 1024))
            GL20.glDeleteShader(shaderID)
            throw IllegalStateException("Failed to compile shader: $path")
        }
        return shaderID
    }

    override fun bind() {
        GL20.glUseProgram(id)
    }

    override fun unbind() {
        GL20.glUseProgram(0)
    }

    override fun destroy() {
        GL20.glDeleteProgram(id)
    }
}
