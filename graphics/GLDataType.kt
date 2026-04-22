package dev.wizard.meta.graphics

enum class GLDataType(val glEnum: Int, val size: Int) {
    GL_BYTE(5120, 1),
    GL_UNSIGNED_BYTE(5121, 1),
    GL_SHORT(5122, 2),
    GL_UNSIGNED_SHORT(5123, 2),
    GL_INT(5124, 4),
    GL_UNSIGNED_INT(5125, 4),
    GL_FLOAT(5126, 4)
}