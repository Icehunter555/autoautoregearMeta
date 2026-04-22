package dev.wizard.meta.graphics

import net.minecraft.util.EnumFacing

object GeoMasks {
    val FACEMAP = hashMapOf(
        EnumFacing.DOWN to 1,
        EnumFacing.UP to 2,
        EnumFacing.NORTH to 4,
        EnumFacing.SOUTH to 8,
        EnumFacing.WEST to 16,
        EnumFacing.EAST to 32
    )

    object Line {
        const val DOWN_WEST = 17
        const val UP_WEST = 18
        const val DOWN_EAST = 33
        const val UP_EAST = 34
        const val DOWN_NORTH = 5
        const val UP_NORTH = 6
        const val DOWN_SOUTH = 9
        const val UP_SOUTH = 10
        const val NORTH_WEST = 20
        const val NORTH_EAST = 36
        const val SOUTH_WEST = 24
        const val SOUTH_EAST = 40
        const val ALL = 63
    }

    object Quad {
        const val DOWN = 1
        const val UP = 2
        const val NORTH = 4
        const val SOUTH = 8
        const val WEST = 16
        const val EAST = 32
        const val ALL = 63
    }
}
