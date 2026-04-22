package dev.wizard.meta.util.combat

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.module.modules.client.CombatSetting
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.util.math.Vec3d
import kotlin.math.max

class ExposureSample(val width: Float, val height: Float) {
    private val array: Array<Point>
    val maxY: Double

    init {
        val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
        val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)
        val gridXZ = width * gridMultiplierXZ
        val gridY = height * gridMultiplierY
        val sizeXZ = MathUtilKt.floorToInt(1.0 / gridMultiplierXZ)
        val sizeY = MathUtilKt.floorToInt(1.0 / gridMultiplierY)
        val xzOffset = (1.0 - gridMultiplierXZ * sizeXZ) / 2.0
        
        val list = mutableListOf<Point>()
        var maxYTemp = 0.0
        
        for (yIndex in 0..sizeY) {
            for (xIndex in 0..sizeXZ) {
                for (zIndex in 0..sizeXZ) {
                    if (!CombatSetting.horizontalCenterSampling && xIndex != 0 && xIndex != sizeXZ && zIndex != 0 && zIndex != sizeXZ) continue
                    if (!CombatSetting.verticalCenterSampling && yIndex != 0 && yIndex != sizeY && (xIndex != 0 && xIndex != sizeXZ || zIndex != 0 && zIndex != sizeXZ)) continue
                    
                    var mask = 0
                    if (yIndex == 0) mask = 1 else if (yIndex == sizeY) mask = 2
                    if (xIndex == 0) mask = mask or 16 else if (xIndex == sizeXZ) mask = mask or 32
                    if (zIndex == 0) mask = mask or 4 else if (zIndex == sizeXZ) mask = mask or 8
                    
                    val x = gridXZ * xIndex + xzOffset
                    val y = gridY * yIndex
                    val z = gridXZ * zIndex + xzOffset
                    maxYTemp = max(y, maxYTemp)
                    list.add(Point(x, y, z, mask))
                }
            }
        }
        maxY = maxYTemp
        array = list.toTypedArray()
    }

    fun offset(x: Double, y: Double, z: Double): Array<Vec3d> {
        return Array(array.size) { i ->
            val point = array[i]
            Vec3d(point.x + x, point.y + y, point.z + z)
        }
    }

    fun getMask(index: Int): Int = array[index].mask

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExposureSample) return false
        return width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        return 31 * width.hashCode() + height.hashCode()
    }

    companion object {
        private val samplePointsCache = Long2ObjectOpenHashMap<ExposureSample>()

        fun getExposureSample(width: Float, height: Float): ExposureSample {
            val key = (width.toRawBits().toLong() shl 32) or (height.toRawBits().toLong() and 0xFFFFFFFFL)
            return samplePointsCache[key] ?: synchronized(samplePointsCache) {
                samplePointsCache.get(key) ?: ExposureSample(width, height).also {
                    samplePointsCache.put(key, it)
                }
            }
        }

        fun resetSamplePoints() {
            samplePointsCache.clear()
        }
    }

    private class Point(val x: Double, val y: Double, val z: Double, val mask: Int)
}
