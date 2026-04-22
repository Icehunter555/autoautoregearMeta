package dev.wizard.meta.mixins.accessor.render

import net.minecraft.client.renderer.EntityRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(EntityRenderer::class)
interface AccessorEntityRenderer {
    @Invoker("renderHand")
    fun invokeRenderHand(var1: Float, var2: Int)
}
