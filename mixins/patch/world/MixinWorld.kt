package dev.wizard.meta.mixins.patch.world

import dev.wizard.meta.util.world.RaytraceKt
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow

@Mixin(World::class)
abstract class MixinWorld {
    @Shadow
    abstract fun getBlockState(var1: BlockPos): IBlockState

    @Overwrite
    fun rayTraceBlocks(vec31: Vec3d, vec32: Vec3d, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): RayTraceResult? {
        return RaytraceKt.rayTrace(this as World, vec31, vec32, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)
    }
}
