package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.renderer.entity.RenderLivingBase
import net.minecraft.entity.EntityLivingBase
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(RenderLivingBase::class)
abstract class MixinRenderEntityLivingBase<T : EntityLivingBase> {
    @Inject(method = ["setBrightness"], at = [At("HEAD")], cancellable = true)
    private fun removeDamageTint(entitylivingbaseIn: T, partialTicks2: Float, combineTextures: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (NoRender.isEnabled && NoRender.noEntityHurtOverlay && (entitylivingbaseIn.hurtTime > 0 || entitylivingbaseIn.deathTime > 0)) {
            cir.returnValue = false
        }
    }
}
