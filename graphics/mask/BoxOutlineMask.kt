package dev.wizard.meta.graphics.mask

@JvmInline
value class BoxOutlineMask(val mask: Int) {
    val isEmpty: Boolean get() = mask == 0

    infix fun or(other: BoxOutlineMask): BoxOutlineMask = BoxOutlineMask(mask or other.mask)
    operator fun plus(other: BoxOutlineMask): BoxOutlineMask = this or other
    operator fun minus(other: BoxOutlineMask): BoxOutlineMask = BoxOutlineMask(mask and other.mask.inv())

    fun contains(other: BoxOutlineMask): Boolean = (mask.inv() and other.mask) == 0

    fun toVertexMask(): BoxVertexMask {
        var result = BoxVertexMask.EMPTY
        if (contains(DOWN_NORTH)) result = result or BoxVertexMask.DOWN_NORTH
        if (contains(DOWN_SOUTH)) result = result or BoxVertexMask.DOWN_SOUTH
        if (contains(DOWN_WEST)) result = result or BoxVertexMask.DOWN_WEST
        if (contains(DOWN_EAST)) result = result or BoxVertexMask.DOWN_EAST
        if (contains(UP_NORTH)) result = result or BoxVertexMask.UP_NORTH
        if (contains(UP_SOUTH)) result = result or BoxVertexMask.UP_SOUTH
        if (contains(UP_WEST)) result = result or BoxVertexMask.UP_WEST
        if (contains(UP_EAST)) result = result or BoxVertexMask.UP_EAST
        if (contains(NORTH_WEST)) result = result or BoxVertexMask.NORTH_WEST
        if (contains(NORTH_EAST)) result = result or BoxVertexMask.NORTH_EAST
        if (contains(SOUTH_WEST)) result = result or BoxVertexMask.SOUTH_WEST
        if (contains(SOUTH_EAST)) result = result or BoxVertexMask.SOUTH_EAST
        return result
    }

    companion object {
        val EMPTY = BoxOutlineMask(0)
        val DOWN_NORTH = BoxOutlineMask(1)
        val DOWN_SOUTH = BoxOutlineMask(2)
        val DOWN_WEST = BoxOutlineMask(4)
        val DOWN_EAST = BoxOutlineMask(8)
        val UP_NORTH = BoxOutlineMask(16)
        val UP_SOUTH = BoxOutlineMask(32)
        val UP_WEST = BoxOutlineMask(64)
        val UP_EAST = BoxOutlineMask(128)
        val NORTH_WEST = BoxOutlineMask(256)
        val NORTH_EAST = BoxOutlineMask(512)
        val SOUTH_WEST = BoxOutlineMask(1024)
        val SOUTH_EAST = BoxOutlineMask(2048)

        val DOWN = DOWN_NORTH or DOWN_SOUTH or DOWN_WEST or DOWN_EAST
        val UP = UP_NORTH or UP_SOUTH or UP_WEST or UP_EAST
        val NORTH = DOWN_NORTH or UP_NORTH or NORTH_WEST or NORTH_EAST
        val SOUTH = DOWN_SOUTH or UP_SOUTH or SOUTH_WEST or SOUTH_EAST
        val WEST = DOWN_WEST or UP_WEST or NORTH_WEST or SOUTH_WEST
        val EAST = DOWN_EAST or UP_EAST or NORTH_EAST or SOUTH_EAST

        val ALL = DOWN or UP or NORTH or SOUTH or WEST or EAST
    }
}
