package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.event.events.AddCollisionBoxEvent
import dev.wizard.meta.module.modules.render.XRay
import net.minecraft.block.Block
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BlockStateContainer.StateImplementation::class)
class MixinStateImplementation {
    @Shadow
    @Final
    private lateinit var block: Block

    @Inject(method = ["addCollisionBoxToList"], at = [At("HEAD")])
    fun addCollisionBoxToList(worldIn: World, pos: BlockPos, entityBox: AxisAlignedBB, collidingBoxes: MutableList<AxisAlignedBB>, entityIn: Entity?, isActualState: Boolean, ci: CallbackInfo) {
        AddCollisionBoxEvent(entityIn, entityBox, pos, this.block, collidingBoxes).post()
    }

    @Inject(method = ["shouldSideBeRendered"], at = [At("HEAD")], cancellable = true)
    fun shouldSideBeRendered(blockAccess: IBlockAccess, pos: BlockPos, facing: EnumFacing, ci: CallbackInfoReturnable<Boolean>) {
        if (XRay.shouldReplace(blockAccess.getBlockState(pos.offset(facing)))) {
            ci.returnValue = true
        }
    }
}
