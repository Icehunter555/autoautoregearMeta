package dev.wizard.meta.util.world

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.math.vector.toVec3dCenter
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.*

fun World.canBreakBlock(pos: BlockPos): Boolean {
    val blockState = getBlockState(pos)
    return blockState.block === Blocks.AIR || blockState.getBlockHardness(this, pos) != -1.0f
}

fun World.getGroundPos(entity: Entity): BlockPos {
    return getGroundPos(entity.entityBoundingBox)
}

fun World.getGroundPos(boundingBox: AxisAlignedBB): BlockPos {
    val center = boundingBox.center
    val cx = MathUtilKt.floorToInt(center.x)
    val cz = MathUtilKt.floorToInt(center.z)
    var rx = cx
    var ry = Int.MIN_VALUE
    var rz = cz
    
    val pos = BlockPos.PooledMutableBlockPos.retain()
    for (x in MathUtilKt.floorToInt(boundingBox.minX + 0.01)..MathUtilKt.floorToInt(boundingBox.maxX - 0.01)) {
        for (z in MathUtilKt.floorToInt(boundingBox.minZ + 0.01)..MathUtilKt.floorToInt(boundingBox.maxZ - 0.01)) {
            for (y in MathUtilKt.floorToInt(boundingBox.minY - 0.5) downTo -1) {
                if (y < ry) break
                pos.setPos(x, y, z)
                val box = getBlockState(pos).getCollisionBoundingBox(this, pos)
                if (box != null) {
                    if (ry == Int.MIN_VALUE || y > ry || (y == ry && (MathUtilKt.getSq(x - cx) < MathUtilKt.getSq(rx - cx) || MathUtilKt.getSq(z - cz) < MathUtilKt.getSq(rz - cz)))) {
                        rx = x
                        ry = y
                        rz = z
                    }
                }
            }
        }
    }
    pos.release()
    return BlockPos(rx, if (ry == Int.MIN_VALUE) -999 else ry, rz)
}

fun World.getGroundLevel(entity: Entity): Double {
    return getGroundLevel(entity.entityBoundingBox)
}

fun World.getGroundLevel(boundingBox: AxisAlignedBB): Double {
    var maxY = Double.NEGATIVE_INFINITY
    val pos = BlockPos.PooledMutableBlockPos.retain()
    for (x in MathUtilKt.floorToInt(boundingBox.minX + 0.01)..MathUtilKt.floorToInt(boundingBox.maxX - 0.01)) {
        for (z in MathUtilKt.floorToInt(boundingBox.minZ + 0.01)..MathUtilKt.floorToInt(boundingBox.maxZ - 0.01)) {
            for (y in MathUtilKt.floorToInt(boundingBox.minY - 0.5) downTo -1) {
                if (y < ceil(maxY).toInt() - 1) break
                pos.setPos(x, y, z)
                val box = getCollisionBox(pos)
                if (box != null) {
                    maxY = max(maxY, y.toDouble() + box.maxY)
                }
            }
        }
    }
    pos.release()
    return maxY
}

fun World.isVisible(pos: BlockPos, tolerance: Double = 1.0): Boolean {
    val player = Wrapper.player ?: return false
    val center = pos.toVec3dCenter()
    val result = rayTraceBlocks(player.getPositionEyes(1.0f), center, false, true, false)
    return result != null && (result.blockPos == pos || (result.hitVec != null && result.hitVec.distanceTo(center) <= tolerance))
}

fun World.isLiquid(pos: BlockPos): Boolean {
    return getBlockState(pos).isLiquid
}

fun World.isWater(pos: BlockPos): Boolean {
    return getBlockState(pos).isWater
}

fun SafeClientEvent.hasNeighbor(pos: BlockPos): Boolean {
    return EnumFacing.values().any { !world.getBlockState(pos.offset(it)).isReplaceable }
}

fun World.isPlaceable(pos: BlockPos, ignoreSelfCollide: Boolean = false): Boolean {
    return getBlockState(pos).isReplaceable && EntityManager.checkNoEntityCollision(AxisAlignedBB(pos), if (ignoreSelfCollide) Wrapper.player else null)
}

fun World.checkBlockCollision(pos: BlockPos, box: AxisAlignedBB, tolerance: Double = 0.005): Boolean {
    val blockBox = getCollisionBox(pos) ?: return false
    return box.intersects(
        pos.x + blockBox.minX + tolerance, pos.y + blockBox.minY + tolerance, pos.z + blockBox.minZ + tolerance,
        pos.x + blockBox.maxX - tolerance, pos.y + blockBox.maxY - tolerance, pos.z + blockBox.maxZ - tolerance
    )
}
