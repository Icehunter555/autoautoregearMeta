package dev.wizard.meta.util.math

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object RotationUtils {
    fun calcAbsAngleDiff(a: Float, b: Float): Float {
        return abs(a - b) % 180.0f
    }

    fun calcAngleDiff(a: Float, b: Float): Float {
        return normalizeAngle(a - b)
    }

    fun SafeClientEvent.faceEntityClosest(entity: Entity) {
        val rotation = getRotationToEntityClosest(entity)
        player.setRotation(rotation)
    }

    fun SafeClientEvent.getRelativeRotation(entity: Entity): Float {
        return getRelativeRotation(EntityUtils.getEyePosition(entity))
    }

    fun SafeClientEvent.getRelativeRotation(posTo: Vec3d): Float {
        return getRotationDiff(getRotationTo(posTo), Vec2f(player))
    }

    fun getRotationDiff(r1: Vec2f, r2: Vec2f): Float {
        val r1Rad = r1.toRadians()
        val r2Rad = r2.toRadians()
        return MathUtilKt.toDegree(acos(cos(r1Rad.y) * cos(r2Rad.y) * cos(r1Rad.x - r2Rad.x) + sin(r1Rad.y) * sin(r2Rad.y)).toFloat())
    }

    fun SafeClientEvent.getRotationToEntityClosest(entity: Entity): Vec2f {
        val box = entity.entityBoundingBox
        val eyePos = EntityUtils.getEyePosition(player)
        if (player.entityBoundingBox.intersects(box)) {
            return getRotationTo(eyePos, box.center)
        }
        val x = eyePos.x.coerceIn(box.minX, box.maxX)
        val y = eyePos.y.coerceIn(box.minY, box.maxY)
        val z = eyePos.z.coerceIn(box.minZ, box.maxZ)
        return getRotationTo(eyePos, Vec3d(x, y, z))
    }

    fun SafeClientEvent.getRotationToEntity(entity: Entity): Vec2f {
        return getRotationTo(entity.positionVector)
    }

    fun SafeClientEvent.getRotationTo(posTo: Vec3d): Vec2f {
        return getRotationTo(player.getPositionEyes(1.0f), posTo)
    }

    fun SafeClientEvent.getYawTo(posTo: Vec3d): Float {
        val vec = posTo.subtract(EntityUtils.getEyePosition(player))
        return normalizeAngle(MathUtilKt.toDegree(atan2(vec.z, vec.x)).toFloat() - 90.0f)
    }

    fun getRotationTo(posFrom: Vec3d, posTo: Vec3d): Vec2f {
        val x = posTo.x - posFrom.x
        val y = posTo.y - posFrom.y
        val z = posTo.z - posFrom.z
        val xz = sqrt(x * x + z * z)
        val yaw = normalizeAngle(MathUtilKt.toDegree(atan2(z, x)).toDouble() - 90.0)
        val pitch = normalizeAngle(-MathUtilKt.toDegree(atan2(y, xz)).toDouble())
        return Vec2f(yaw.toFloat(), pitch.toFloat())
    }

    fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn % 360.0
        if (angle >= 180.0) angle -= 360.0
        if (angle < -180.0) angle += 360.0
        return angle
    }

    fun normalizeAngle(angleIn: Float): Float {
        var angle = angleIn % 360.0f
        if (angle >= 180.0f) angle -= 360.0f
        if (angle < -180.0f) angle += 360.0f
        return angle
    }

    fun EntityPlayerSP.setRotation(rotation: Vec2f) {
        setYaw(rotation.x)
        setPitch(rotation.y)
    }

    fun EntityPlayerSP.setYaw(yaw: Float) {
        rotationYaw += normalizeAngle(yaw - rotationYaw)
    }

    fun EntityPlayerSP.setPitch(pitch: Float) {
        rotationPitch = (rotationPitch + normalizeAngle(pitch - rotationPitch)).coerceIn(-90.0f, 90.0f)
    }

    fun EntityPlayerSP.legitRotation(rotation: Vec2f): Vec2f {
        return Vec2f(legitYaw(rotation.x), legitPitch(rotation.y))
    }

    fun EntityPlayerSP.legitYaw(yaw: Float): Float {
        return rotationYaw + normalizeAngle(yaw - rotationYaw)
    }

    fun EntityPlayerSP.legitPitch(pitch: Float): Float {
        return (rotationPitch + normalizeAngle(pitch - rotationPitch)).coerceIn(-90.0f, 90.0f)
    }

    fun EnumFacing.getYaw(): Float {
        return when (this) {
            EnumFacing.NORTH -> -180.0f
            EnumFacing.SOUTH -> 0.0f
            EnumFacing.EAST -> -90.0f
            EnumFacing.WEST -> 90.0f
            else -> 0.0f
        }
    }
}
