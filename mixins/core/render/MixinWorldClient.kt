package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.multiplayer.WorldClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(WorldClient::class)
class MixinWorldClient {
    @ModifyVariable(method = ["showBarrierParticles(IIIILjava/util/Random;ZLnet/minecraft/util/math/BlockPos$MutableBlockPos;)V"], at = At("HEAD"), argsOnly = true)
    fun showBarrierParticlesHook(holdingBarrier: Boolean): Boolean {
        if (NoRender.isEnabled && NoRender.showBarriers) {
            return true
        }
        return holdingBarrier
    }
}
