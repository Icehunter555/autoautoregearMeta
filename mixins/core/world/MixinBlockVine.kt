package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.movement.NoSlowDown
import net.minecraft.block.Block
import net.minecraft.block.BlockVine
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockVine::class)
class MixinBlockVine {
    @Inject(method = ["getCollisionBoundingBox"], at = [At("HEAD")], cancellable = true)
    private fun removeCollisionBox(blockState: IBlockState, worldIn: IBlockAccess, pos: BlockPos, cir: CallbackInfoReturnable<AxisAlignedBB>) {
        if (NoSlowDown.isEnabled && NoSlowDown.vines) {
            cir.returnValue = Block.NULL_AABB
        }
    }
}
