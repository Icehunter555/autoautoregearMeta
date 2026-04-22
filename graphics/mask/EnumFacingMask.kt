package dev.wizard.meta.graphics.mask

import net.minecraft.util.EnumFacing

object EnumFacingMask {
    const val DOWN = 1
    const val UP = 2
    const val NORTH = 4
    const val SOUTH = 8
    const val WEST = 16
    const val EAST = 32
    const val ALL = 63

    fun getMaskForSide(side: EnumFacing): Int = when (side) {
        EnumFacing.DOWN -> DOWN
        EnumFacing.UP -> UP
        EnumFacing.NORTH -> NORTH
        EnumFacing.SOUTH -> SOUTH
        EnumFacing.WEST -> WEST
        EnumFacing.EAST -> EAST
    }
}
