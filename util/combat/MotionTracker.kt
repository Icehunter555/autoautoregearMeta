package dev.wizard.meta.util.combat

import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.Wrapper
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class MotionTracker(val entity: Entity) {
    private val motionLog = ArrayDeque<Vec3d>()
    var prevMotion: Vec3d = Vec3d.ZERO
        private set
    var motion: Vec3d = Vec3d.ZERO
        private set

    fun tick() {
        synchronized(this) {
            motionLog.add(calcActualMotion(entity))
            while (motionLog.size > 5) {
                motionLog.removeFirst()
            }
            prevMotion = motion
            motion = calcAverageMotion()
        }
    }

    private fun calcActualMotion(entity: Entity): Vec3d {
        return Vec3d(entity.posX - entity.prevPosX, entity.posY - entity.prevPosY, entity.posZ - entity.prevPosZ)
    }

    private fun calcAverageMotion(): Vec3d {
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0
        for (m in motionLog) {
            sumX += m.x
            sumY += m.y
            sumZ += m.z
        }
        return Vec3d(sumX, sumY, sumZ).scale(1.0 / motionLog.size)
    }

    fun calcPosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3d {
        val relativePos = calcRelativePosAhead(ticksAhead, interpolation)
        val partialTicks = if (interpolation) RenderUtils3D.getPartialTicks() else 1.0f
        return EntityUtils.getInterpolatedPos(entity, partialTicks).add(relativePos)
    }

    fun calcRelativePosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3d {
        val world = Wrapper.world ?: return Vec3d.ZERO
        val partialTicks = if (interpolation) RenderUtils3D.getPartialTicks().toDouble() else 1.0
        val averageMotion = prevMotion.add(motion.subtract(prevMotion).scale(partialTicks))
        var movedVec = Vec3d.ZERO
        
        for (ticks in 0..ticksAhead) {
            val bbox = entity.entityBoundingBox
            val nextFull = movedVec.add(averageMotion)
            if (canMove(world, bbox, nextFull)) {
                movedVec = nextFull
            } else {
                val nextXZ = movedVec.add(Vec3d(averageMotion.x, 0.0, averageMotion.z))
                if (canMove(world, bbox, nextXZ)) {
                    movedVec = nextXZ
                } else {
                    val nextY = movedVec.add(Vec3d(0.0, averageMotion.y, 0.0))
                    if (canMove(world, bbox, nextY)) {
                        movedVec = nextY
                    } else {
                        break
                    }
                }
            }
        }
        return movedVec
    }

    private fun canMove(world: World, bbox: AxisAlignedBB, offset: Vec3d): Boolean {
        return !world.collidesWithAnyBlock(bbox.offset(offset))
    }
}
