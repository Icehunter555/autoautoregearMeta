package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.event.events.render.RenderEntityEvent
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.LocalCapture

@Mixin(value = [RenderManager::class], priority = 114514)
class MixinRenderManager {
    @Inject(method = ["renderEntity"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;setRenderOutlines(Z)V", shift = At.Shift.BEFORE)], cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    fun renderEntityPre(entity: Entity?, x: Double, y: Double, z: Double, yaw: Float, partialTicks2: Float, debug: Boolean, ci: CallbackInfo, render: Render<Entity>?) {
        if (entity == null || render == null || !RenderEntityEvent.renderingEntities) {
            return
        }
        val eventAll = RenderEntityEvent.All.Pre(entity, x, y, z, yaw, partialTicks2, render)
        eventAll.post()
        if (eventAll.isCancelled) {
            ci.cancel()
        } else if (entity !is EntityLivingBase) {
            val eventModel = RenderEntityEvent.Model.Pre.of(entity, x, y, z, yaw, partialTicks2, render)
            eventModel.post()
        }
    }

    @Inject(method = ["renderEntity"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V", shift = At.Shift.AFTER)], locals = LocalCapture.CAPTURE_FAILHARD)
    fun renderEntityPeri(entity: Entity?, x: Double, y: Double, z: Double, yaw: Float, partialTicks2: Float, debug: Boolean, ci: CallbackInfo, render: Render<Entity>?) {
        if (entity == null || render == null || !RenderEntityEvent.renderingEntities) {
            return
        }
        if (entity !is EntityLivingBase) {
            val eventModel = RenderEntityEvent.Model.Post.of(entity, x, y, z, yaw, partialTicks2, render)
            eventModel.post()
        }
    }

    @Inject(method = ["renderEntity"], at = [At("RETURN")], locals = LocalCapture.CAPTURE_FAILHARD)
    fun renderEntityPost(entity: Entity?, x: Double, y: Double, z: Double, yaw: Float, partialTicks2: Float, debug: Boolean, ci: CallbackInfo, render: Render<Entity>?) {
        if (entity == null || render == null || !RenderEntityEvent.renderingEntities) {
            return
        }
        val event = RenderEntityEvent.All.Post(entity, x, y, z, yaw, partialTicks2, render)
        event.post()
    }
}
