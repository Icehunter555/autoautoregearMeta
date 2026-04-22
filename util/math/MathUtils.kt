package dev.wizard.meta.util.math

import dev.wizard.meta.util.math.vector.Vec2d
import net.minecraft.util.math.Vec3d
import javax.vecmath.Vector3f
import java.util.*
import kotlin.math.*

object MathUtils {
    fun Boolean.toInt(): Int = if (this) 1 else 0

    fun Double.roundToPlaces(places: Int): Double {
        val scale = 10.0.pow(places)
        return rint(this * scale) / scale
    }

    fun Boolean.toIntSign(): Int = if (this) 1 else -1

    fun floorToInt(value: Double): Int = floor(value).toInt()

    fun ceilToInt(value: Double): Int = ceil(value).toInt()

    fun IntRange.random(): Int {
        return Random().nextInt(last + 1 - first) + first
    }

    fun clamp(value: Double, min: Double, max: Double): Double {
        return value.coerceIn(min, max)
    }

    fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }

    fun lerp(a: Vec3d, b: Vec3d, t: Double): Vec3d {
        return Vec3d(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t))
    }

    fun lerp(a: Vec2d, b: Vec2d, t: Double): Vec2d {
        return Vec2d(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
    }

    @JvmStatic
    fun ceilToPOT(valueIn: Int): Int {
        var i = valueIn - 1
        i = i or (i shr 1)
        i = i or (i shr 2)
        i = i or (i shr 4)
        i = i or (i shr 8)
        i = i or (i shr 16)
        return i + 1
    }

    @JvmStatic
    fun round(value: Float, places: Int): Float {
        val scale = 10.0f.pow(places)
        return rint((value * scale).toDouble()).toFloat() / scale
    }

    @JvmStatic
    fun round(value: Double, places: Int): Double {
        val scale = 10.0.pow(places)
        return rint(value * scale) / scale
    }

    @JvmStatic
    fun decimalPlaces(value: Double): Int {
        val parts = value.toString().split('.')
        return if (parts.size > 1) parts[1].length else 0
    }

    @JvmStatic
    fun decimalPlaces(value: Float): Int {
        val parts = value.toString().split('.')
        return if (parts.size > 1) parts[1].length else 0
    }

    @JvmStatic
    fun isNumberEven(i: Int): Boolean = (i and 1) == 0

    @JvmStatic
    fun reverseNumber(num: Int, min: Int, max: Int): Int = max + min - num

    @JvmStatic
    fun convertRange(valueIn: Int, minIn: Int, maxIn: Int, minOut: Int, maxOut: Int): Int {
        return convertRange(valueIn.toDouble(), minIn.toDouble(), maxIn.toDouble(), minOut.toDouble(), maxOut.toDouble()).toInt()
    }

    @JvmStatic
    fun convertRange(valueIn: Float, minIn: Float, maxIn: Float, minOut: Float, maxOut: Float): Float {
        return convertRange(valueIn.toDouble(), minIn.toDouble(), maxIn.toDouble(), minOut.toDouble(), maxOut.toDouble()).toFloat()
    }

    @JvmStatic
    fun convertRange(valueIn: Double, minIn: Double, maxIn: Double, minOut: Double, maxOut: Double): Double {
        val rangeIn = maxIn - minIn
        val rangeOut = maxOut - minOut
        val convertedIn = (valueIn - minIn) * (rangeOut / rangeIn) + minOut
        val actualMin = min(minOut, maxOut)
        val actualMax = max(minOut, maxOut)
        return convertedIn.coerceIn(actualMin, actualMax)
    }

    @JvmStatic
    fun lerp(from: Double, to: Double, delta: Double): Double = from + (to - from) * delta

    @JvmStatic
    fun lerp(from: Float, to: Float, delta: Float): Float = from + (to - from) * delta

    @JvmStatic
    fun approxEq(a: Double, b: Double, epsilon: Double = 1.0E-4): Boolean = abs(a - b) < epsilon

    @JvmStatic
    fun approxEq(a: Float, b: Float, epsilon: Float = 1.0E-4f): Boolean = abs(a - b) < epsilon

    @JvmStatic
    fun frac(value: Double): Double = value - floor(value)

    @JvmStatic
    fun frac(value: Float): Float = value - floor(value.toDouble()).toFloat()

    @JvmStatic
    fun mix(first: Vector3f, second: Vector3f, factor: Float): Vector3f {
        return Vector3f(
            first.x * (1.0f - factor) + second.x * factor,
            first.y * (1.0f - factor) + second.y * factor,
            first.z * (1.0f - factor) + first.z * factor
        )
    }

    @JvmStatic
    fun normalize(value: Double, minIn: Double, maxIn: Double, minOut: Double, maxOut: Double): Double {
        return lerp(minOut, maxOut, (value - minIn) / (maxIn - minIn))
    }
}
