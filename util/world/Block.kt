package dev.wizard.meta.util.world

import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.inventory.blockBlacklist
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

val IBlockState.isBlacklisted: Boolean
    get() = blockBlacklist.contains(block)

val IBlockState.isLiquid: Boolean
    get() = material.isLiquid

val IBlockState.isWater: Boolean
    get() = block === Blocks.WATER

val IBlockState.isReplaceable: Boolean
    get() = material.isReplaceable

val IBlockState.isFullBox: Boolean
    get() = getCollisionBoundingBox(Wrapper.world ?: return false, BlockPos.ORIGIN) == Block.FULL_BLOCK_AABB

fun World.getBlockState(x: Int, y: Int, z: Int): IBlockState {
    return if (y in 0..255) {
        val chunk = getChunkFromChunkCoords(x shr 4, z shr 4)
        if (chunk.isEmpty) {
            Blocks.AIR.defaultState
        } else {
            chunk.getBlockState(x, y, z)
        }
    } else {
        Blocks.AIR.defaultState
    }
}

val IBlockState.isAir: Boolean
    get() = material === Material.AIR

fun World.isAir(x: Int, y: Int, z: Int): Boolean {
    return getBlockState(x, y, z).isAir
}

fun World.isAir(pos: BlockPos): Boolean {
    return getBlockState(pos).isAir
}

fun World.getBlock(pos: BlockPos): Block {
    return getBlockState(pos).block
}

fun World.getMaterial(pos: BlockPos): Material {
    return getBlockState(pos).material
}

fun World.getSelectedBox(pos: BlockPos): AxisAlignedBB {
    return getBlockState(pos).getSelectedBoundingBox(this, pos)
}

fun World.getCollisionBox(pos: BlockPos): AxisAlignedBB? {
    return getBlockState(pos).getCollisionBoundingBox(this, pos)
}

fun World.hasCollisionBox(pos: BlockPos): Boolean {
    return getCollisionBox(pos) != null
}
