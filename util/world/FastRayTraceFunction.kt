package dev.wizard.meta.util.world

import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface FastRayTraceFunction {
    fun invoke(world: World, pos: BlockPos, state: IBlockState): FastRayTraceAction

    companion object {
        @JvmField
        val DEFAULT = FastRayTraceFunction { world, pos, state ->
            if (state.getCollisionBoundingBox(world, pos) != null) FastRayTraceAction.CALC else FastRayTraceAction.SKIP
        }
    }
}
