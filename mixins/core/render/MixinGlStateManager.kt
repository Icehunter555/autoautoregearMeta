package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.AntiAlias
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GlStateManager::class)
abstract class MixinGlStateManager {
    companion object {
        @Inject(method = ["viewport"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun viewport$Inject$HEAD(x: Int, y: Int, width2: Int, height: Int, ci: CallbackInfo) {
            val sampleLevel = AntiAlias.sampleLevel
            if (sampleLevel == 1.0f) {
                return
            }
            val framebuffer = Minecraft.getMinecraft().framebuffer
            if (GL11.glGetInteger(36006) == framebuffer.framebufferObject) {
                ci.cancel()
                if (x == 0 && y == 0) {
                    GL11.glViewport(x, y, framebuffer.framebufferWidth, framebuffer.framebufferHeight)
                } else {
                    GL11.glViewport((x.toFloat() * sampleLevel).toInt(), (y.toFloat() * sampleLevel).toInt(), (width2.toFloat() * sampleLevel).toInt(), (height.toFloat() * sampleLevel).toInt())
                }
            }
        }

        @Inject(method = ["glLineWidth"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun glLineWidth$Inject$HEAD(width2: Float, ci: CallbackInfo) {
            val sampleLevel = AntiAlias.sampleLevel
            if (sampleLevel != 1.0f) {
                ci.cancel()
                GL11.glLineWidth(width2 * sampleLevel)
            }
        }
    }
}
