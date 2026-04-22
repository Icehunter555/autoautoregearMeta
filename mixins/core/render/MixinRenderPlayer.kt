package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderLivingBase
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.entity.EntityLivingBase
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(RenderPlayer::class)
abstract class MixinRenderPlayer(renderManagerIn: RenderManager, modelBaseIn: ModelBase, shadowSizeIn: Float) : RenderLivingBase<AbstractClientPlayer>(renderManagerIn, modelBaseIn, shadowSizeIn) {
    @Shadow
    protected abstract fun func_177137_d(var1: AbstractClientPlayer)

    @Inject(method = ["doRender"], at = [At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderViewEntity:Lnet/minecraft/entity/Entity;")])
    fun doRenderGetRenderViewEntity(entity: AbstractClientPlayer, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks2: Float, ci: CallbackInfo) {
        if (Freecam.isEnabled && Wrapper.getMinecraft().renderViewEntity !== entity) {
            var renderY = y
            if (entity.isSneaking) {
                renderY = y - 0.125
            }
            this.func_177137_d(entity)
            GlStateManager.enableProfile(GlStateManager.Profile.PLAYER_SKIN)
            super.doRender(entity, x, renderY, z, entityYaw, partialTicks2)
            GlStateManager.disableProfile(GlStateManager.Profile.PLAYER_SKIN)
        }
    }
}
