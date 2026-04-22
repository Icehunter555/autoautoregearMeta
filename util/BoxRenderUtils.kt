package dev.wizard.meta.util

import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.util.math.scale
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object BoxRenderUtils {
    fun calcRiseBox(position: BlockPos, stage: Double): AxisAlignedBB {
        return AxisAlignedBB(
            position.x.toDouble(),
            position.y.toDouble(),
            position.z.toDouble(),
            position.x.toDouble() + 1.0,
            position.y.toDouble() + stage,
            position.z.toDouble() + 1.0
        )
    }

    fun calcFallBox(position: BlockPos, stage: Double): AxisAlignedBB {
        return AxisAlignedBB(
            position.x.toDouble(),
            position.y.toDouble() + (1.0 - stage),
            position.z.toDouble(),
            position.x.toDouble() + 1.0,
            position.y.toDouble() + 1.0,
            position.z.toDouble() + 1.0
        )
    }

    fun calcGrowBox(position: BlockPos, stage: Double): AxisAlignedBB {
        val prog = Easing.OUT_CUBIC.inc(stage.toFloat())
        return AxisAlignedBB(position).scale(prog.toDouble())
    }

    fun calcShrinkBox(position: BlockPos, stage: Double): AxisAlignedBB {
        val prog = Easing.IN_CUBIC.inc(stage.toFloat())
        return AxisAlignedBB(position).scale(prog.toDouble())
    }

    fun calcFadeAlpha(fadeTime: Long = 2000L, startTime: Long): Int {
        return ((fadeTime - (System.currentTimeMillis() - startTime)).toFloat() / fadeTime.toFloat() * 255f).toInt().coerceIn(0, 255)
    }

    fun calcFadeAlpha(fadeTime: Int = 2000, startTime: Long): Int {
        return ((fadeTime.toLong() - (System.currentTimeMillis() - startTime)).toFloat() / fadeTime.toFloat() * 255f).toInt().coerceIn(0, 255)
    }

    fun calcStage(runTime: Long = 2000L, startTime: Long): Double {
        return ((System.currentTimeMillis() - startTime).toFloat() / runTime.toFloat() * 1000f).toDouble().coerceIn(0.0, 1.0)
    }

    fun isOldFade(fadeTime: Long = 2000L, placedTime: Long): Boolean {
        return System.currentTimeMillis() - placedTime > fadeTime
    }

    fun isOldFade(fadeTime: Int = 2000, placedTime: Long): Boolean {
        return System.currentTimeMillis() - placedTime > fadeTime.toLong()
    }

    fun calcCrunchTop(position: BlockPos, stage: Double): AxisAlignedBB {
        return AxisAlignedBB(
            position.x.toDouble(),
            position.y.toDouble() + (1.0 - stage) / 2.0,
            position.z.toDouble(),
            position.x.toDouble() + 1.0,
            position.y.toDouble() + 1.0,
            position.z.toDouble() + 1.0
        )
    }

    fun calcCrunchBottom(position: BlockPos, stage: Double): AxisAlignedBB {
        return AxisAlignedBB(
            position.x.toDouble(),
            position.y.toDouble(),
            position.z.toDouble(),
            position.x.toDouble() + 1.0,
            position.y.toDouble() + stage / 2.0,
            position.z.toDouble() + 1.0
        )
    }
}
