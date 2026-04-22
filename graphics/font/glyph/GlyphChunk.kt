package dev.wizard.meta.graphics.font.glyph

import dev.wizard.meta.graphics.font.GlyphTexture
import org.lwjgl.opengl.GL11

class GlyphChunk(
    val id: Int,
    val texture: GlyphTexture,
    val charInfoArray: Array<CharInfo>
) {
    private var lodbias: Float = 0.0f

    fun updateLodBias(input: Float) {
        if (input != lodbias) {
            lodbias = input
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 34049, input)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GlyphChunk) return false
        return id == other.id && texture == other.texture
    }

    override fun hashCode(): Int {
        return 31 * id + texture.hashCode()
    }
}
