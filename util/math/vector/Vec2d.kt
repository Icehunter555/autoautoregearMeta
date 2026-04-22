package dev.wizard.meta.util.math.vector

import dev.fastmc.common.DistanceKt
import dev.fastmc.common.MathUtilKt
import net.minecraft.util.math.Vec3d

data class Vec2d(
    val x: Double = 0.0,
    val y: Double = 0.0
) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(vec3d: Vec3d) : this(vec3d.x, vec3d.y)
    constructor(vec2d: Vec2d) : this(vec2d.x, vec2d.y)

    fun toRadians(): Vec2d = Vec2d(MathUtilKt.toRadians(x), MathUtilKt.toRadians(y))

    fun distanceTo(other: Vec2d): Double = DistanceKt.distance(x, y, other.x, other.y)
    fun distanceSqTo(other: Vec2d): Double = DistanceKt.distanceSq(x, y, other.x, other.y)

    fun length(): Double = DistanceKt.length(x, y)
    fun lengthSq(): Double = DistanceKt.lengthSq(x, y)

    operator fun div(other: Vec2d): Vec2d = div(other.x, other.y)
    operator fun div(divider: Double): Vec2d = div(divider, divider)
    fun div(x: Double, y: Double): Vec2d = Vec2d(this.x / x, this.y / y)

    operator fun times(other: Vec2d): Vec2d = times(other.x, other.y)
    operator fun times(multiplier: Double): Vec2d = times(multiplier, multiplier)
    fun times(x: Double, y: Double): Vec2d = Vec2d(this.x * x, this.y * y)

    operator fun minus(other: Vec2d): Vec2d = minus(other.x, other.y)
    operator fun minus(sub: Double): Vec2d = minus(sub, sub)
    fun minus(x: Double, y: Double): Vec2d = plus(-x, -y)

    operator fun plus(other: Vec2d): Vec2d = plus(other.x, other.y)
    operator fun plus(add: Double): Vec2d = plus(add, add)
    fun plus(x: Double, y: Double): Vec2d = Vec2d(this.x + x, this.y + y)

    fun toVec2f(): Vec2f = Vec2f(x.toFloat(), y.toFloat())

    override fun toString(): String = "Vec2d[$x, $y]"

    companion object {
        @JvmField
        val ZERO = Vec2d(0.0, 0.0)
    }
}
