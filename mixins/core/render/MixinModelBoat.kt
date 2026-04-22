package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.movement.BoatFly
import dev.wizard.meta.module.modules.movement.EntitySpeed
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.model.ModelBoat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ModelBoat::class)
class MixinModelBoat {
    @Inject(method = ["render"], at = [At("HEAD")])
    fun render(entityIn: Entity, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scale: Float, info: CallbackInfo) {
        val player = Wrapper.getPlayer()
        if (player.ridingEntity === entityIn && BoatFly.isEnabled) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, BoatFly.opacity)
            GlStateManager.enableBlend()
        }
        if (player.ridingEntity === entityIn && EntitySpeed.isEnabled) {
            GlStateManager.color(1.0f, 1.0f, 1.0f, EntitySpeed.opacity)
            GlStateManager.enableBlend()
        }
    }
}
