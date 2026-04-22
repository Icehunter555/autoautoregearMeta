package dev.wizard.meta.mixins.core.render

import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelBiped
import net.minecraft.client.model.ModelRenderer
import net.minecraft.entity.Entity
import net.minecraft.util.EnumHandSide
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ModelBiped::class)
abstract class MixinModelBiped : ModelBase() {
    @Shadow
    lateinit var field_78116_c: ModelRenderer

    @Shadow
    lateinit var field_78115_e: ModelRenderer

    @Shadow
    lateinit var field_178723_h: ModelRenderer

    @Shadow
    lateinit var field_178724_i: ModelRenderer

    @Shadow
    lateinit var field_178721_j: ModelRenderer

    @Shadow
    lateinit var field_178722_k: ModelRenderer

    @Shadow
    lateinit var field_187075_l: ModelBiped.ArmPose

    @Shadow
    lateinit var field_187076_m: ModelBiped.ArmPose

    @Shadow
    lateinit var field_178720_f: ModelRenderer

    @Shadow
    protected abstract fun func_187072_a(var1: Entity): EnumHandSide

    @Shadow
    protected abstract fun func_187074_a(var1: EnumHandSide): ModelRenderer

    @Inject(method = ["setRotationAngles"], at = [At("HEAD")], cancellable = true)
    fun setRotationAngles(limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scaleFactor: Float, entityIn: Entity, ci: CallbackInfo) {
    }
}
