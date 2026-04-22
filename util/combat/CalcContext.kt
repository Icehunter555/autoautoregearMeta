package dev.wizard.meta.util.combat

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.module.modules.client.CombatSetting
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.world.FastRayTraceAction
import dev.wizard.meta.util.world.FastRayTraceFunction
import dev.wizard.meta.util.world.fastRayTrace
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.EnumDifficulty
import kotlin.math.floor
import kotlin.math.min

class CalcContext(
    val event: SafeClientEvent,
    val entity: EntityLivingBase,
    val predictPos: Vec3d
) {
    val currentPos: Vec3d = entity.positionVector
    val currentBox: AxisAlignedBB = getBoundingBox(entity, currentPos)
    val predictBox: AxisAlignedBB = getBoundingBox(entity, predictPos)
    val clipped: Boolean = event.world.checkBlockCollision(currentBox)
    private val predicting: Boolean = currentPos.distanceSqTo(predictPos) > 0.01
    private val difficulty: EnumDifficulty = event.world.difficulty
    private val reduction: DamageReduction = DamageReduction(entity)
    private val exposureSample: ExposureSample = ExposureSample.getExposureSample(entity.width, entity.height)
    private val samplePoints: Array<Vec3d> = exposureSample.offset(currentBox.minX, currentBox.minY, currentBox.minZ)
    private val samplePointsPredict: Array<Vec3d> = exposureSample.offset(predictBox.minX, predictBox.minY, predictBox.minZ)

    fun checkColliding(pos: Vec3d): Boolean {
        val box = AxisAlignedBB(pos.x - 0.499, pos.y, pos.z - 0.499, pos.x + 0.499, pos.y + 2.0, pos.z + 0.499)
        return !box.intersects(currentBox) && (!predicting || (box.calculateIntercept(currentPos, predictPos) == null && !box.intersects(predictBox)))
    }

    fun calcDamage(crystalX: Double, crystalY: Double, crystalZ: Double, predict: Boolean, mutableBlockPos: BlockPos.MutableBlockPos): Float {
        return calcDamage(crystalX, crystalY, crystalZ, predict, 6.0f, mutableBlockPos)
    }

    fun calcDamage(pos: Vec3d, predict: Boolean, mutableBlockPos: BlockPos.MutableBlockPos): Float {
        return calcDamage(pos.x, pos.y, pos.z, predict, 6.0f, mutableBlockPos)
    }

    fun calcDamage(pos: Vec3d, predict: Boolean, size: Float, mutableBlockPos: BlockPos.MutableBlockPos): Float {
        return calcDamage(pos.x, pos.y, pos.z, predict, size, mutableBlockPos)
    }

    fun calcDamage(crystalX: Double, crystalY: Double, crystalZ: Double, predict: Boolean, size: Float, mutableBlockPos: BlockPos.MutableBlockPos): Float {
        return calcDamage(crystalX, crystalY, crystalZ, predict, size, mutableBlockPos) { world, _, blockState ->
            if (blockState.block !== Blocks.AIR && CrystalUtils.isResistant(blockState)) FastRayTraceAction.CALC else FastRayTraceAction.SKIP
        }
    }

    fun calcDamage(pos: Vec3d, predict: Boolean, mutableBlockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        return calcDamage(pos.x, pos.y, pos.z, predict, 6.0f, mutableBlockPos, function)
    }

    fun calcDamage(pos: Vec3d, predict: Boolean, size: Float, mutableBlockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        return calcDamage(pos.x, pos.y, pos.z, predict, size, mutableBlockPos, function)
    }

    fun calcDamage(crystalX: Double, crystalY: Double, crystalZ: Double, predict: Boolean, size: Float, mutableBlockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        if (difficulty == EnumDifficulty.PEACEFUL) return 0.0f
        val entityPos = if (predict) predictPos else currentPos
        val damage = if (crystalY - entityPos.y > exposureSample.maxY) {
            val state = event.world.getBlockState(mutableBlockPos.setPos(MathUtilKt.floorToInt(crystalX), MathUtilKt.floorToInt(crystalY) - 1, MathUtilKt.floorToInt(crystalZ)))
            if (CrystalUtils.isResistant(state)) 1.0f else calcRawDamage(event, crystalX, crystalY, crystalZ, size, predict, mutableBlockPos, function)
        } else {
            calcRawDamage(event, crystalX, crystalY, crystalZ, size, predict, mutableBlockPos, function)
        }
        return reduction.calcDamage(calcDifficultyDamage(damage), true)
    }

    private fun calcDifficultyDamage(damage: Float): Float {
        if (entity is EntityPlayer) {
            return when (difficulty) {
                EnumDifficulty.PEACEFUL -> 0.0f
                EnumDifficulty.EASY -> min(damage * 0.5f + 1.0f, damage)
                EnumDifficulty.HARD -> damage * 1.5f
                else -> damage
            }
        }
        return damage
    }

    private fun calcRawDamage(event: SafeClientEvent, crystalX: Double, crystalY: Double, crystalZ: Double, size: Float, predict: Boolean, mutableBlockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        val entityPos = if (predict) predictPos else currentPos
        val doubleSize = size * 2.0f
        val scaledDist = entityPos.distanceTo(crystalX, crystalY, crystalZ).toFloat() / doubleSize
        if (scaledDist > 1.0f) return 0.0f
        val factor = (1.0f - scaledDist) * getExposureAmount(event, crystalX, crystalY, crystalZ, predict, mutableBlockPos, function)
        return (floor((factor * factor + factor) * doubleSize * 3.5f + 1.0)).toFloat()
    }

    private fun getExposureAmount(event: SafeClientEvent, crystalX: Double, crystalY: Double, crystalZ: Double, predict: Boolean, mutableBlockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        val box = if (predict) predictBox else currentBox
        if (!clipped && isInside(box, crystalX, crystalY, crystalZ)) return 1.0f
        val points = if (predict) samplePointsPredict else samplePoints
        return if (!CombatSetting.backSideSampling) {
            countSamplePointsOptimized(event, points, box, crystalX, crystalY, crystalZ, mutableBlockPos, function)
        } else {
            countSamplePoints(event, points, crystalX, crystalY, crystalZ, mutableBlockPos, function)
        }
    }

    private fun countSamplePoints(event: SafeClientEvent, samplePoints: Array<Vec3d>, crystalX: Double, crystalY: Double, crystalZ: Double, blockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        var count = 0
        for (samplePoint in samplePoints) {
            if (!event.world.fastRayTrace(samplePoint, crystalX, crystalY, crystalZ, 20, blockPos, function)) {
                count++
            }
        }
        return count.toFloat() / samplePoints.size
    }

    private fun countSamplePointsOptimized(event: SafeClientEvent, samplePoints: Array<Vec3d>, box: AxisAlignedBB, crystalX: Double, crystalY: Double, crystalZ: Double, mutableBlockPos: BlockPos.MutableBlockPos, function: FastRayTraceFunction): Float {
        var count = 0
        var total = 0
        val sideMask = getSideMask(box, crystalX, crystalY, crystalZ)
        for (i in samplePoints.indices) {
            val pointMask = exposureSample.getMask(i)
            if ((sideMask and pointMask) == 0) continue
            total++
            if (!event.world.fastRayTrace(samplePoints[i], crystalX, crystalY, crystalZ, 20, mutableBlockPos, function)) {
                count++
            }
        }
        return count.toFloat() / total
    }

    private fun getSideMask(box: AxisAlignedBB, posX: Double, posY: Double, posZ: Double): Int {
        var mask = 0
        if (posX < box.minX) mask = 16 else if (posX > box.maxX) mask = 32
        if (posY < box.minY) mask = mask or 1 else if (posY > box.maxY) mask = mask or 2
        if (posZ < box.minZ) mask = mask or 4 else if (posZ > box.maxZ) mask = mask or 8
        return mask
    }

    private fun isInside(box: AxisAlignedBB, x: Double, y: Double, z: Double): Boolean {
        return x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY && z >= box.minZ && z <= box.maxZ
    }

    private fun getBoundingBox(entity: EntityLivingBase, pos: Vec3d): AxisAlignedBB {
        val halfWidth = min(entity.width.toDouble(), 2.0) / 2.0
        val height = min(entity.height.toDouble(), 3.0)
        return AxisAlignedBB(pos.x - halfWidth, pos.y, pos.z - halfWidth, pos.x + halfWidth, pos.y + height, pos.z + halfWidth)
    }
}
