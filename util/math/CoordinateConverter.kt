package dev.wizard.meta.util.math

import dev.wizard.meta.manager.managers.WaypointManager
import net.minecraft.util.math.BlockPos

object CoordinateConverter {
    fun BlockPos.asString(): String {
        return "$x, $y, $z"
    }

    fun toCurrent(dimension: Int, pos: BlockPos): BlockPos {
        if (dimension == WaypointManager.genDimension()) return pos
        return when (dimension) {
            -1 -> toOverworld(pos)
            0 -> toNether(pos)
            else -> pos
        }
    }

    fun bothConverted(dimension: Int, pos: BlockPos): String {
        return when (dimension) {
            -1 -> "${toOverworld(pos).asString()} (${pos.asString()})"
            0 -> "${pos.asString()} (${toNether(pos).asString()})"
            else -> pos.asString()
        }
    }

    fun toNether(pos: BlockPos): BlockPos {
        return BlockPos(pos.x / 8, pos.y, pos.z / 8)
    }

    fun toOverworld(pos: BlockPos): BlockPos {
        return BlockPos(pos.x * 8, pos.y, pos.z * 8)
    }
}
