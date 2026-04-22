package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.movement.Velocity
import dev.wizard.meta.module.modules.player.Interactions
import dev.wizard.meta.util.Wrapper
import net.minecraft.block.BlockLiquid
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockLiquid::class)
class MixinBlockLiquid {
    @Inject(method = ["modifyAcceleration"], at = [At("HEAD")], cancellable = true)
    fun modifyAcceleration(worldIn: World, pos: BlockPos, entityIn: Entity, motion: Vec3d, cir: CallbackInfoReturnable<Vec3d>) {
        if (worldIn !== Wrapper.getWorld() || entityIn !== Wrapper.getPlayer()) {
            return
        }
        if (Velocity.shouldCancelLiquidVelocity()) {
            cir.returnValue = motion
        }
    }

    @Inject(method = ["canCollideCheck"], at = [At("HEAD")], cancellable = true)
    fun canCollideCheck(blockState: IBlockState, hitIfLiquid: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (Interactions.isLiquidInteractEnabled()) {
            cir.returnValue = true
        }
    }
}
