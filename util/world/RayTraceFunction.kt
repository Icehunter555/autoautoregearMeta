package dev.wizard.meta.util.world

import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface RayTraceFunction {
    fun invoke(world: World, pos: BlockPos, state: IBlockState): RayTraceAction

    companion object {
        @JvmField
        val DEFAULT = RayTraceFunction { _, _, state ->
            if (state.block !== Blocks.AIR) RayTraceAction.Calc else RayTraceAction.Skip
        }
    }
}
