package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.tileentity.TileEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(TileEntityRendererDispatcher::class)
class MixinTileEntityRendererDispatcher {
    @Inject(method = ["render(Lnet/minecraft/tileentity/TileEntity;FI)V"], at = [At("HEAD")], cancellable = true)
    fun render$Inject$HEAD(tileEntity: TileEntity, partialTicks2: Float, destroyStage: Int, ci: CallbackInfo) {
        if (NoRender.isEnabled) {
            NoRender.handleTileEntity(tileEntity, ci)
        }
    }
}
