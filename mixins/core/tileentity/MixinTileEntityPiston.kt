package dev.wizard.meta.mixins.core.tileentity

import dev.wizard.meta.module.modules.movement.Velocity
import net.minecraft.tileentity.TileEntityPiston
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(TileEntityPiston::class)
class MixinTileEntityPiston {
    @Inject(method = ["moveCollidedEntities"], at = [At("HEAD")], cancellable = true)
    private fun cancelPiston(p_184322_1_: Float, ci: CallbackInfo) {
        if (Velocity.shouldCancelPiston()) {
            ci.cancel()
        }
    }
}
