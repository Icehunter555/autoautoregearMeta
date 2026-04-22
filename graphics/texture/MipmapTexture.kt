package dev.wizard.meta.graphics.texture

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.awt.image.BufferedImage

class MipmapTexture(bufferedImage: BufferedImage, format: Int, levels: Int) : AbstractTexture() {
    override val width: Int = bufferedImage.width
    override val height: Int = bufferedImage.height

    init {
        genTexture()
        bindTexture()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 33082, 0)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 33083, levels)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 33084, 0)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, 33085, levels)
        TextureUtils.uploadRGBA(bufferedImage, format)
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
        unbindTexture()
    }
}
