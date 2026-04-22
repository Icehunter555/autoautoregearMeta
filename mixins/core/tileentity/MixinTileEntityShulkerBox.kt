package dev.wizard.meta.mixins.core.tileentity

import dev.wizard.meta.module.modules.movement.Velocity
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntityShulkerBox
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(TileEntityShulkerBox::class)
abstract class MixinTileEntityShulkerBox {
    @Inject(method = ["moveCollidedEntities"], at = [At("HEAD")], cancellable = true)
    fun moveCollidedEntitiesHook(ci: CallbackInfo) {
        if (Velocity.shouldCancelShulkerPush()) {
            ci.cancel()
        }
    }

    @Inject(method = ["getTopBoundingBox"], at = [At("HEAD")], cancellable = true)
    fun resetTopBoundingBox(p_190588_1_: EnumFacing, cir: CallbackInfoReturnable<AxisAlignedBB>) {
        if (Velocity.shouldCancelShulkerPush()) {
            cir.returnValue = Block.FULL_BLOCK_AABB
        }
    }
}
