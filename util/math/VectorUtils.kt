package dev.wizard.meta.util.math

import dev.fastmc.common.BlockPosUtil
import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.*

object VectorUtils {
    fun getBlockPosInSphere(entity: Entity, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(entity.posX, entity.posY, entity.posZ, radius)
    }

    fun getBlockPosInSphere(pos: Vec3d, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(pos.x, pos.y, pos.z, radius)
    }

    fun getBlockPosInSphere(cx: Double, cy: Double, cz: Double, radius: Float): Sequence<BlockPos> = sequence {
        val squaredRadius = radius * radius
        val mutablePos = BlockPos.MutableBlockPos()
        val xRange = getAxisRange(cx, radius)
        val yRange = getAxisRange(cy, radius)
        val zRange = getAxisRange(cz, radius)
        
        for (x in xRange) {
            for (y in yRange) {
                for (z in zRange) {
                    mutablePos.setPos(x, y, z)
                    if (mutablePos.distanceSq(cx, cy, cz) <= squaredRadius) {
                        yield(mutablePos.toImmutable())
                    }
                }
            }
        }
    }

    private fun getAxisRange(d: Double, r: Float): IntRange {
        return MathUtilKt.floorToInt(d - r)..MathUtilKt.ceilToInt(d + r)
    }

    fun Vec2f.toViewVec(): Vec3d {
        val yawRad = MathUtilKt.toRadians(x.toDouble())
        val pitchRad = MathUtilKt.toRadians(y.toDouble())
        
        val yaw = -yawRad - PI
        val pitch = -pitchRad
        
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = -cos(pitch)
        val sinPitch = sin(pitch)
        
        return Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch)
    }

    fun Vec3i.multiply(multiplier: Int): Vec3i {
        return Vec3i(x * multiplier, y * multiplier, z * multiplier)
    }

    fun Vec3d.times(other: Vec3d): Vec3d {
        return Vec3d(x * other.x, y * other.y, z * other.z)
    }

    fun Vec3d.times(multiplier: Double): Vec3d {
        return Vec3d(x * multiplier, y * multiplier, z * multiplier)
    }

    fun Vec3d.plus(other: Vec3d): Vec3d = add(other)
    fun Vec3d.minus(other: Vec3d): Vec3d = subtract(other)

    fun BlockPos.MutableBlockPos.setAndAdd(set: Vec3i, add: Vec3i): BlockPos.MutableBlockPos {
        return setPos(set.x + add.x, set.y + add.y, set.z + add.z)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: Vec3i, x: Int, y: Int, z: Int): BlockPos.MutableBlockPos {
        return setPos(set.x + x, set.y + y, set.z + z)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, side: EnumFacing): BlockPos.MutableBlockPos {
        return setAndAdd(set, side.directionVec)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, side: EnumFacing, n: Int): BlockPos.MutableBlockPos {
        val dirVec = side.directionVec
        return setPos(set.x + dirVec.x * n, set.y + dirVec.y * n, set.z + dirVec.z * n)
    }

    fun BlockPos.toLong(): Long = BlockPosUtil.toLong(x, y, z)
    fun toLong(x: Int, y: Int, z: Int): Long = BlockPosUtil.toLong(x, y, z)
    fun toLong(x: Double, y: Double, z: Double): Long = BlockPosUtil.toLong(MathUtilKt.floorToInt(x), MathUtilKt.floorToInt(y), MathUtilKt.floorToInt(z))

    fun fromLong(l: Long): BlockPos = BlockPos(BlockPosUtil.xFromLong(l), BlockPosUtil.yFromLong(l), BlockPosUtil.zFromLong(l))
    fun xFromLong(l: Long): Int = BlockPosUtil.xFromLong(l)
    fun yFromLong(l: Long): Int = BlockPosUtil.yFromLong(l)
    fun zFromLong(l: Long): Int = BlockPosUtil.zFromLong(l)
}
