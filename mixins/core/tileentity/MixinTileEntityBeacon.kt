package dev.wizard.meta.mixins.core.tileentity

import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.tileentity.TileEntityBeacon
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(TileEntityBeacon::class)
class MixinTileEntityBeacon {
    @Inject(method = ["shouldBeamRender"], at = [At("HEAD")], cancellable = true)
    fun shouldBeamRender(returnable: CallbackInfoReturnable<Float>) {
        if (NoRender.isEnabled && NoRender.beaconBeams) {
            returnable.returnValue = 0.0f
            returnable.cancel()
        }
    }
}
