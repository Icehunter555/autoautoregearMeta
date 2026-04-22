package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.renderer.entity.layers.LayerArmorBase
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.EntityEquipmentSlot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LayerArmorBase::class)
abstract class MixinLayerArmorBase {
    @Inject(method = ["renderArmorLayer"], at = [At("HEAD")], cancellable = true)
    fun renderArmorLayerPre(entityLivingBaseIn: EntityLivingBase, limbSwing: Float, limbSwingAmount: Float, partialTicks2: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scale: Float, slotIn: EntityEquipmentSlot, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.shouldHide(entityLivingBaseIn, slotIn)) {
            ci.cancel()
        }
    }
}
