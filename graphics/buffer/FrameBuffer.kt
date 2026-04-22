package dev.wizard.meta.graphics.buffer

import dev.wizard.meta.graphics.GLObject
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.texture.TextureUtil
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

open class FrameBuffer : GLObject {
    override val id: Int = GL30.glGenFramebuffers()
    private val textureID: Int = TextureUtil.glGenTextures()

    open fun allocateFrameBuffer(width: Int, height: Int) {
        GlStateManager.bindTexture(textureID)
        GL11.glTexImage2D(3553, 0, 32856, width, height, 0, 6408, 5121, null)
        GL11.glTexParameteri(3553, 10241, 9729)
        GL11.glTexParameteri(3553, 10240, 9729)
        GlStateManager.bindTexture(0)
        GL30.glFramebufferTexture2D(36160, 36064, 3553, textureID, 0)
    }

    fun bindTexture() {
        GlStateManager.bindTexture(textureID)
    }

    fun unbindTexture() {
        GlStateManager.bindTexture(0)
    }

    override fun bind() {
        GL30.glBindFramebuffer(36160, id)
    }

    override fun unbind() {
        GL30.glBindFramebuffer(36160, 0)
    }

    override fun destroy() {
        GL30.glDeleteFramebuffers(id)
        OpenGlHelper.deleteTextures(textureID)
    }
}
