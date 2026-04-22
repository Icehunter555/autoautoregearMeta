package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.gui.MapItemRenderer
import net.minecraft.world.storage.MapData
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MapItemRenderer::class)
class MixinMapItemRenderer {
    @Inject(method = ["renderMap"], at = [At("HEAD")], cancellable = true)
    fun renderMap(mapdataIn: MapData, noOverlayRendering: Boolean, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.map) {
            ci.cancel()
        }
    }
}
