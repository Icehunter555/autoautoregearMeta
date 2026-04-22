package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.AntiAlias
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.nio.ByteBuffer

@Mixin(Framebuffer::class)
abstract class MixinFramebuffer {
    @Shadow
    var field_147621_c = 0
    @Shadow
    var field_147618_d = 0
    @Shadow
    var field_147620_b = 0
    @Shadow
    var field_147622_a = 0
    @Shadow
    var field_147616_f = 0
    @Shadow
    var field_147617_g = 0
    @Shadow
    var field_147619_e = false
    @Shadow
    var field_147624_h = 0
    @Shadow(remap = false)
    private var stencilEnabled = false

    @Shadow
    abstract fun func_147614_f()

    @Shadow
    abstract fun func_147607_a(var1: Int)

    @Inject(method = ["createFramebuffer"], at = [At("HEAD")], cancellable = true)
    fun createBindFramebuffer$inject$HEAD(width2: Int, height: Int, ci: CallbackInfo) {
        if (AntiAlias.isDisabled) {
            return
        }
        ci.cancel()
        val level = AntiAlias.sampleLevel
        this.create((width2.toFloat() * level).toInt(), (height.toFloat() * level).toInt())
    }

    private fun create(width2: Int, height: Int) {
        this.field_147621_c = width2
        this.field_147618_d = height
        this.field_147622_a = width2
        this.field_147620_b = height
        this.field_147616_f = GL30.glGenFramebuffers()
        GL30.glBindFramebuffer(36160, this.field_147616_f)
        this.field_147617_g = TextureUtil.glGenTextures()
        this.func_147607_a(9729)
        GlStateManager.bindTexture(this.field_147617_g)
        GL11.glTexImage2D(3553, 0, 32856, width2, height, 0, 6408, 5121, null as ByteBuffer?)
        GL30.glFramebufferTexture2D(36160, 36064, 3553, this.field_147617_g, 0)
        if (this.field_147619_e) {
            this.field_147624_h = GL30.glGenRenderbuffers()
            GL30.glBindRenderbuffer(36161, this.field_147624_h)
            if (!this.stencilEnabled) {
                GL30.glRenderbufferStorage(36161, 33190, width2, height)
                GL30.glFramebufferRenderbuffer(36160, 36096, 36161, this.field_147624_h)
            } else {
                GL30.glRenderbufferStorage(36161, 35056, width2, height)
                GL30.glFramebufferRenderbuffer(36160, 36096, 36161, this.field_147624_h)
                GL30.glFramebufferRenderbuffer(36160, 36128, 36161, this.field_147624_h)
            }
        }
        GlStateManager.bindTexture(0)
        if (this.field_147619_e) {
            GL30.glBindRenderbuffer(36161, 0)
        }
        this.func_147614_f()
    }
}
