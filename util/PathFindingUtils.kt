package dev.wizard.meta.util

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.math.vector.toBlockPos
import dev.wizard.meta.util.world.getGroundPos
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.minecraft.util.math.BlockPos
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

object PathFindingUtils {
    fun SafeClientEvent.pathToBlockPos(
        targetPos: BlockPos,
        stepHeight: Int = 2,
        fallHeight: Int = 5,
        scanHRange: Int = 16,
        scanVRange: Int = 16,
        timeout: Int = 10000
    ): Deque<PathFinder.PathNode>? {
        val blocks = scanWorldBlocks(scanHRange, scanVRange)
        val pathFinder = PathFinder(blocks, stepHeight, fallHeight)
        val startPos = world.getGroundPos(player).up()
        val start = startPos.toNode()
        val end = targetPos.toNode()
        return try {
            pathFinder.calculatePath(start, end, timeout)
        } catch (e: Exception) {
            null
        }
    }

    private fun SafeClientEvent.scanWorldBlocks(scanHRange: Int, scanVRange: Int): LongSet {
        val set = LongOpenHashSet()
        val playerPos = player.positionVector.toBlockPos()
        val mutablePos = BlockPos.MutableBlockPos()
        for (x in -scanHRange..scanHRange) {
            for (z in -scanHRange..scanHRange) {
                for (y in -scanVRange..scanVRange) {
                    val pos = mutablePos.setPos(playerPos.x + x, playerPos.y + y, playerPos.z + z)
                    if (!world.isOutsideBuildHeight(pos) && world.worldBorder.contains(pos)) {
                        val blockState = world.getBlockState(pos)
                        if (blockState.getCollisionBoundingBox(world, pos) != null) {
                            set.add(pos.toLong())
                        }
                    }
                }
            }
        }
        return set
    }

    private fun BlockPos.toNode(): PathFinder.Node {
        return PathFinder.Node(x, y, z)
    }

    fun getPathDistance(path: Deque<PathFinder.PathNode>): Double {
        if (path.isEmpty()) return 0.0
        var distance = 0.0
        var previous: PathFinder.PathNode? = null
        for (node in path) {
            previous?.let {
                val dx = node.x - it.x
                val dy = node.y - it.y
                val dz = node.z - it.z
                distance += sqrt((dx * dx + dy * dy + dz * dz).toDouble())
            }
            previous = node
        }
        return distance
    }

    fun SafeClientEvent.isPositionReached(target: PathFinder.PathNode, tolerance: Double = 0.5): Boolean {
        val dx = abs(player.posX - (target.x + 0.5))
        val dy = abs(player.posY - target.y.toDouble())
        val dz = abs(player.posZ - (target.z + 0.5))
        return dx <= tolerance && dy <= tolerance && dz <= tolerance
    }
}
