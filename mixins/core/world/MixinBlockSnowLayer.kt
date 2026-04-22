package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.movement.NoSlowDown
import net.minecraft.block.Block
import net.minecraft.block.BlockSnow
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockSnow::class)
class MixinBlockSnowLayer {
    @Inject(method = ["getCollisionBoundingBox"], at = [At("HEAD")], cancellable = true)
    private fun cancelSnow(blockState: IBlockState, worldIn: IBlockAccess, pos: BlockPos, cir: CallbackInfoReturnable<AxisAlignedBB>) {
        if (NoSlowDown.noSnow && NoSlowDown.isEnabled) {
            cir.returnValue = Block.NULL_AABB
        }
    }
}
