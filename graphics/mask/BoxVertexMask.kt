package dev.wizard.meta.graphics.mask

import net.minecraft.util.EnumFacing

@JvmInline
value class BoxVertexMask(val mask: Int) {
    val isEmpty: Boolean get() = mask == 0

    infix fun or(other: BoxVertexMask): BoxVertexMask = BoxVertexMask(mask or other.mask)
    operator fun plus(other: BoxVertexMask): BoxVertexMask = this or other
    operator fun minus(other: BoxVertexMask): BoxVertexMask = BoxVertexMask(mask and other.mask.inv())

    fun contains(other: BoxVertexMask): Boolean = (mask.inv() and other.mask) == 0

    fun countBits(digit: Int): Int {
        var sum = 0
        for (i in 1..digit) {
            if ((mask and (1 shl i)) != 0) {
                sum++
            }
        }
        return sum
    }

    fun toOutlineMask(): BoxOutlineMask {
        var result = BoxOutlineMask.EMPTY
        if (contains(DOWN)) result = result or BoxOutlineMask.DOWN
        if (contains(UP)) result = result or BoxOutlineMask.UP
        if (contains(NORTH)) result = result or BoxOutlineMask.NORTH
        if (contains(SOUTH)) result = result or BoxOutlineMask.SOUTH
        if (contains(WEST)) result = result or BoxOutlineMask.WEST
        if (contains(EAST)) result = result or BoxOutlineMask.EAST
        return result
    }

    companion object {
        val EMPTY = BoxVertexMask(0)
        val XN_YN_ZN = BoxVertexMask(1)
        val XN_YN_ZP = BoxVertexMask(2)
        val XN_YP_ZN = BoxVertexMask(4)
        val XN_YP_ZP = BoxVertexMask(8)
        val XP_YN_ZN = BoxVertexMask(16)
        val XP_YN_ZP = BoxVertexMask(32)
        val XP_YP_ZN = BoxVertexMask(64)
        val XP_YP_ZP = BoxVertexMask(128)

        val DOWN = XN_YN_ZN or XN_YN_ZP or XP_YN_ZN or XP_YN_ZP
        val UP = XN_YP_ZN or XN_YP_ZP or XP_YP_ZN or XP_YP_ZP
        val NORTH = XN_YN_ZN or XN_YP_ZN or XP_YN_ZN or XP_YP_ZN
        val SOUTH = XN_YN_ZP or XN_YP_ZP or XP_YN_ZP or XP_YP_ZP
        val WEST = XN_YN_ZN or XN_YN_ZP or XN_YP_ZN or XN_YP_ZP
        val EAST = XP_YN_ZN or XP_YN_ZP or XP_YP_ZN or XP_YP_ZP

        val ALL = DOWN or UP or NORTH or SOUTH or WEST or EAST

        val DOWN_NORTH = DOWN or NORTH
        val DOWN_SOUTH = DOWN or SOUTH
        val DOWN_WEST = DOWN or WEST
        val DOWN_EAST = DOWN or EAST
        val UP_NORTH = UP or NORTH
        val UP_SOUTH = UP or SOUTH
        val UP_WEST = UP or WEST
        val UP_EAST = UP or EAST
        val NORTH_WEST = NORTH or WEST
        val NORTH_EAST = NORTH or EAST
        val SOUTH_WEST = SOUTH or WEST
        val SOUTH_EAST = SOUTH or EAST

        fun getMaskForSide(side: EnumFacing): BoxVertexMask = when (side) {
            EnumFacing.DOWN -> DOWN
            EnumFacing.UP -> UP
            EnumFacing.NORTH -> NORTH
            EnumFacing.SOUTH -> SOUTH
            EnumFacing.WEST -> WEST
            EnumFacing.EAST -> EAST
        }
    }
}
