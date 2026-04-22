package dev.wizard.meta.util.math.vector

import dev.fastmc.common.DistanceKt
import dev.fastmc.common.MathUtilKt
import net.minecraft.entity.Entity
import kotlin.math.sqrt

@JvmInline
value class Vec2f(val bits: Long) {
    constructor(x: Float, y: Float) : this((x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFFFFFFL))
    constructor(entity: Entity) : this(entity.rotationYaw, entity.rotationPitch)
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(vec2d: Vec2d) : this(vec2d.x.toFloat(), vec2d.y.toFloat())

    val x: Float get() = Float.fromBits((bits shr 32).toInt())
    val y: Float get() = Float.fromBits((bits and 0xFFFFFFFFL).toInt())

    fun toRadians(): Vec2f = Vec2f(MathUtilKt.toRadians(x), MathUtilKt.toRadians(y))

    fun distanceSqTo(other: Vec2f): Float = DistanceKt.distanceSq(x, y, other.x, other.y)
    fun distanceTo(other: Vec2f): Double = DistanceKt.distance(x, y, other.x, other.y).toDouble()

    fun length(): Float = sqrt(lengthSq())
    fun lengthSq(): Float = x * x + y * y

    operator fun div(other: Vec2f): Vec2f = div(other.x, other.y)
    operator fun div(divider: Float): Vec2f = div(divider, divider)
    fun div(x: Float, y: Float): Vec2f = Vec2f(this.x / x, this.y / y)

    operator fun times(other: Vec2f): Vec2f = times(other.x, other.y)
    operator fun times(multiplier: Float): Vec2f = times(multiplier, multiplier)
    fun times(x: Float, y: Float): Vec2f = Vec2f(this.x * x, this.y * y)

    operator fun minus(other: Vec2f): Vec2f = minus(other.x, other.y)
    operator fun minus(value: Float): Vec2f = minus(value, value)
    fun minus(x: Float, y: Float): Vec2f = plus(-x, -y)

    operator fun plus(other: Vec2f): Vec2f = plus(other.x, other.y)
    operator fun plus(value: Float): Vec2f = plus(value, value)
    fun plus(x: Float, y: Float): Vec2f = Vec2f(this.x + x, this.y + y)

    fun toVec2d(): Vec2d = Vec2d(x.toDouble(), y.toDouble())

    override fun toString(): String = "Vec2f(bits=$bits)"

    companion object {
        val ZERO = Vec2f(0.0f, 0.0f)

        @JvmStatic fun getX(bits: Long): Float = Float.fromBits((bits shr 32).toInt())
        @JvmStatic fun getY(bits: Long): Float = Float.fromBits((bits and 0xFFFFFFFFL).toInt())
    }
}
