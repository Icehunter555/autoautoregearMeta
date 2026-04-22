package dev.wizard.meta.util.math.vector

import dev.fastmc.common.MathUtilKt
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

private val NUM_X_BITS = 1 + MathHelper.calculateLogBase2(MathHelper.smallestEncompassingPowerOfTwo(30000000))
private val NUM_Z_BITS = NUM_X_BITS
private val NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS
private val Y_SHIFT = NUM_Z_BITS
private val X_SHIFT = Y_SHIFT + NUM_Y_BITS
private val X_MASK = (1L shl NUM_X_BITS) - 1L
private val Y_MASK = (1L shl NUM_Y_BITS) - 1L
private val Z_MASK = (1L shl NUM_Z_BITS) - 1L

fun Vec3d.toBlockPos(xOffset: Int = 0, yOffset: Int = 0, zOffset: Int = 0): BlockPos {
    return BlockPos(MathUtilKt.floorToInt(x) + xOffset, MathUtilKt.floorToInt(y) + yOffset, MathUtilKt.floorToInt(z) + zOffset)
}

fun Vec3i.toVec3dCenter(xOffset: Double = 0.0, yOffset: Double = 0.0, zOffset: Double = 0.0): Vec3d {
    return toVec3d(0.5 + xOffset, 0.5 + yOffset, 0.5 + zOffset)
}

fun Vec3i.toVec3d(): Vec3d = Vec3d(this)

fun Vec3i.toVec3d(offset: Vec3d): Vec3d {
    return toVec3d(offset.x, offset.y, offset.z)
}

fun Vec3i.toVec3d(offset: Vec3f): Vec3d {
    return toVec3d(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
}

fun Vec3i.toVec3d(xOffset: Double, yOffset: Double, zOffset: Double): Vec3d {
    return Vec3d(x.toDouble() + xOffset, y.toDouble() + yOffset, z.toDouble() + zOffset)
}

fun toLong(x: Int, y: Int, z: Int): Long {
    return (x.toLong() and X_MASK shl X_SHIFT) or (y.toLong() and Y_MASK shl Y_SHIFT) or (z.toLong() and Z_MASK)
}
