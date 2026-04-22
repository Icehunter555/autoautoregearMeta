package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.movement.NoSlowDown
import net.minecraft.block.BlockWeb
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(BlockWeb::class)
class MixinBlockWeb {
    @Inject(method = ["onEntityCollision"], at = [At("HEAD")], cancellable = true)
    fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity, info: CallbackInfo) {
        if (NoSlowDown.isEnabled && NoSlowDown.cobweb) {
            info.cancel()
        }
    }
}
