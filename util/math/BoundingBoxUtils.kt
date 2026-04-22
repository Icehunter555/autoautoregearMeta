package dev.wizard.meta.util.math

import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.util.math.vector.toVec3d
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.min

val AxisAlignedBB.xCenter: Double get() = minX + xLength * 0.5
val AxisAlignedBB.yCenter: Double get() = minY + yLength * 0.5
val AxisAlignedBB.zCenter: Double get() = minZ + zLength * 0.5

val AxisAlignedBB.xLength: Double get() = maxX - minX
val AxisAlignedBB.yLength: Double get() = maxY - minY
val AxisAlignedBB.zLength: Double get() = maxZ - minZ

val AxisAlignedBB.lengths: Vec3d get() = Vec3d(xLength, yLength, zLength)

fun AxisAlignedBB.corners(scale: Double): Array<Vec3d> {
    val growSizes = lengths.times(scale - 1.0)
    return grow(growSizes.x, growSizes.y, growSizes.z).corners()
}

fun AxisAlignedBB.corners(): Array<Vec3d> {
    return arrayOf(
        Vec3d(minX, minY, minZ),
        Vec3d(minX, minY, maxZ),
        Vec3d(minX, maxY, minZ),
        Vec3d(minX, maxY, maxZ),
        Vec3d(maxX, minY, minZ),
        Vec3d(maxX, minY, maxZ),
        Vec3d(maxX, maxY, minZ),
        Vec3d(maxX, maxY, maxZ)
    )
}

fun AxisAlignedBB.side(side: EnumFacing, scale: Double = 0.5): Vec3d {
    val sideDirectionVec = side.directionVec.toVec3d()
    val vec = lengths.times(sideDirectionVec).times(scale)
    return center.add(vec)
}

fun AxisAlignedBB.scale(multiplier: Double): AxisAlignedBB {
    return scale(multiplier, multiplier, multiplier)
}

fun AxisAlignedBB.scale(x: Double, y: Double, z: Double): AxisAlignedBB {
    val halfXLength = xLength * 0.5
    val halfYLength = yLength * 0.5
    val halfZLength = zLength * 0.5
    return grow(halfXLength * (x - 1.0), halfYLength * (y - 1.0), halfZLength * (z - 1.0))
}

fun AxisAlignedBB.limitSize(x: Double, y: Double, z: Double): AxisAlignedBB {
    val halfX = min(xLength, x) / 2.0
    val halfY = min(yLength, y) / 2.0
    val halfZ = min(zLength, z) / 2.0
    val c = center
    return AxisAlignedBB(c.x - halfX, c.y - halfY, c.z - halfZ, c.x + halfX, c.y + halfY, c.z + halfZ)
}

fun AxisAlignedBB.intersectsBlock(x: Int, y: Int, z: Int): Boolean {
    return intersects(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
}

fun AxisAlignedBB.intersectsBlock(pos: BlockPos): Boolean {
    return intersectsBlock(pos.x, pos.y, pos.z)
}

fun AxisAlignedBB.isInSight(posFrom: Vec3d = PlayerPacketManager.position, rotation: Rotation = PlayerPacketManager.rotation, range: Double = 8.0): Boolean {
    return isInSight(posFrom, rotation.toViewVec(), range)
}

fun AxisAlignedBB.isInSight(posFrom: Vec3d, viewVec: Vec3d, range: Double = 4.25): Boolean {
    val sightEnd = posFrom.add(viewVec.scale(range))
    return grow(AntiCheat.placeRotationBoundingBoxGrow.toDouble()).calculateIntercept(posFrom, sightEnd) != null
}
