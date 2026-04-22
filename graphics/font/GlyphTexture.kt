package dev.wizard.meta.graphics.font

import dev.wizard.meta.graphics.texture.AbstractTexture
import org.lwjgl.opengl.GL45

class GlyphTexture(override val width: Int, override val height: Int, levels: Int) : AbstractTexture() {
    init {
        val id = GL45.glCreateTextures(3553)
        textureID = id
        GL45.glTextureParameteri(id, 33082, 0)
        GL45.glTextureParameteri(id, 33083, levels)
        GL45.glTextureParameteri(id, 33084, 0)
        GL45.glTextureParameteri(id, 33085, levels)
        GL45.glTextureParameteri(id, 10242, 33071)
        GL45.glTextureParameteri(id, 10243, 33071)
        GL45.glTextureParameteri(id, 10240, 9728)
        GL45.glTextureParameteri(id, 10241, 9987)
        GL45.glTextureParameterf(id, 34049, 0.0f)
        GL45.glTextureStorage2D(id, levels + 1, 36283, width, height)
    }
}
