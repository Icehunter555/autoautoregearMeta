package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.module.modules.render.Nametags
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.entity.RenderLivingBase
import net.minecraft.entity.EntityLivingBase
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [RenderLivingBase::class], priority = 114514)
abstract class MixinRenderLivingBase<T : EntityLivingBase> {
    @Shadow
    protected lateinit var field_77045_g: ModelBase

    @Inject(method = ["renderName*"], at = [At("HEAD")], cancellable = true)
    protected fun renderName$Inject$HEAD(entity: T, x: Double, y: Double, z: Double, ci: CallbackInfo) {
        if (Nametags.isEnabled && Nametags.checkEntityType(entity)) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderModel"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.BEFORE)])
    fun renderModelHead(entity: T, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scaleFactor: Float, ci: CallbackInfo) {
        if (entity == null || !RenderEntityEvent.renderingEntities) {
            return
        }
        val eventModel = RenderEntityEvent.Model.Pre.of(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, this.field_77045_g)
        eventModel.post()
    }

    @Inject(method = ["renderModel"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.AFTER)])
    fun renderEntityReturn(entity: T, limbSwing: Float, limbSwingAmount: Float, ageInTicks: Float, netHeadYaw: Float, headPitch: Float, scaleFactor: Float, ci: CallbackInfo) {
        if (entity == null || !RenderEntityEvent.renderingEntities) {
            return
        }
        val eventModel = RenderEntityEvent.Model.Post.of(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, this.field_77045_g)
        eventModel.post()
    }
}
