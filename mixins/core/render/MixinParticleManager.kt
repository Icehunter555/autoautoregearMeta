package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ParticleManager::class)
class MixinParticleManager {
    @Inject(method = ["addEffect"], at = [At("HEAD")], cancellable = true)
    fun addEffect(effect: Particle, ci: CallbackInfo) {
        if (NoRender.shouldHideParticles(effect)) {
            ci.cancel()
        }
    }
}
