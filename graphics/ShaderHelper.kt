package dev.wizard.meta.graphics

import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.ResolutionUpdateEvent
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.ShaderGroup
import net.minecraft.client.shader.ShaderLinkHelper
import net.minecraft.util.ResourceLocation

class ShaderHelper(shaderIn: ResourceLocation) : AlwaysListening {
    private val mc: Minecraft = Wrapper.minecraft
    val shader: ShaderGroup?
    private var frameBuffersInitialized = false

    init {
        shader = if (!OpenGlHelper.shadersSupported) {
            MetaMod.logger.warn("Shaders are unsupported by OpenGL!")
            null
        } else {
            try {
                ShaderLinkHelper.setNewStaticShaderLinkHelper()
                ShaderGroup(mc.textureManager, mc.resourceManager, mc.framebuffer, shaderIn).apply {
                    createBindFramebuffers(mc.displayWidth, mc.displayHeight)
                }
            } catch (e: Exception) {
                MetaMod.logger.warn("Failed to load shaders")
                e.printStackTrace()
                null
            }
        }

        listener<TickEvent.Post> {
            if (!frameBuffersInitialized) {
                shader?.createBindFramebuffers(mc.displayWidth, mc.displayHeight)
                frameBuffersInitialized = true
            }
        }

        listener<ResolutionUpdateEvent> {
            shader?.createBindFramebuffers(it.width, it.height)
        }
    }

    fun getFrameBuffer(name: String): Framebuffer? {
        return shader?.getFramebufferRaw(name)
    }
}
