package dev.wizard.meta.graphics.texture

import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

abstract class AbstractTexture {
    var textureID: Int = -1
        protected set

    abstract val width: Int
    abstract val height: Int

    fun genTexture() {
        textureID = GL11.glGenTextures()
    }

    fun bindTexture() {
        if (textureID != -1) {
            GlStateManager.func_179144_i(textureID)
        }
    }

    fun unbindTexture() {
        GlStateManager.func_179144_i(0)
    }

    fun deleteTexture() {
        if (textureID != -1) {
            GlStateManager.func_179150_h(textureID)
            textureID = -1
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is AbstractTexture && this.textureID == other.textureID
    }

    override fun hashCode(): Int {
        return textureID
    }
}
