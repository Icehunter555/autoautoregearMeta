package dev.wizard.meta.graphics.mask

import net.minecraft.util.EnumFacing

@JvmInline
value class SideMask(val mask: Int) {
    val isEmpty: Boolean get() = mask == 0

    operator fun plus(other: SideMask): SideMask = SideMask(mask or other.mask)
    operator fun minus(other: SideMask): SideMask = SideMask(mask and other.mask.inv())

    fun contains(other: SideMask): Boolean = (mask.inv() and other.mask) == 0

    fun toVertexMask(): BoxVertexMask {
        var result = BoxVertexMask.EMPTY
        if (contains(DOWN)) result += BoxVertexMask.DOWN
        if (contains(UP)) result += BoxVertexMask.UP
        if (contains(NORTH)) result += BoxVertexMask.NORTH
        if (contains(SOUTH)) result += BoxVertexMask.SOUTH
        if (contains(WEST)) result += BoxVertexMask.WEST
        if (contains(EAST)) result += BoxVertexMask.EAST
        return result
    }

    fun toOutlineMask(): BoxOutlineMask {
        var result = BoxOutlineMask.EMPTY
        if (contains(DOWN)) result += BoxOutlineMask.DOWN
        if (contains(UP)) result += BoxOutlineMask.UP
        if (contains(NORTH)) result += BoxOutlineMask.NORTH
        if (contains(SOUTH)) result += BoxOutlineMask.SOUTH
        if (contains(WEST)) result += BoxOutlineMask.WEST
        if (contains(EAST)) result += BoxOutlineMask.EAST
        return result
    }

    fun toOutlineMaskInv(): BoxOutlineMask {
        var result = BoxOutlineMask.ALL
        if (!contains(DOWN)) result -= BoxOutlineMask.DOWN
        if (!contains(UP)) result -= BoxOutlineMask.UP
        if (!contains(NORTH)) result -= BoxOutlineMask.NORTH
        if (!contains(SOUTH)) result -= BoxOutlineMask.SOUTH
        if (!contains(WEST)) result -= BoxOutlineMask.WEST
        if (!contains(EAST)) result -= BoxOutlineMask.EAST
        return result
    }

    companion object {
        val EMPTY = SideMask(0)
        val DOWN = SideMask(1)
        val UP = SideMask(2)
        val NORTH = SideMask(4)
        val SOUTH = SideMask(8)
        val WEST = SideMask(16)
        val EAST = SideMask(32)
        val ALL = SideMask(63)

        fun EnumFacing.toMask(): SideMask = when (this) {
            EnumFacing.DOWN -> DOWN
            EnumFacing.UP -> UP
            EnumFacing.NORTH -> NORTH
            EnumFacing.SOUTH -> SOUTH
            EnumFacing.WEST -> WEST
            EnumFacing.EAST -> EAST
        }
    }
}
