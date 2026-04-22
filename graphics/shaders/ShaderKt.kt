package dev.wizard.meta.graphics.shaders

import org.lwjgl.opengl.GL20

inline fun <T : Shader> T.use(block: T.() -> Unit) {
    bind()
    block()
    GL20.glUseProgram(0)
}
