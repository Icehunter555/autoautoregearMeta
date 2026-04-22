package dev.wizard.meta.graphics

import dev.wizard.meta.util.math.MathUtils
import kotlin.math.*

enum class Easing {
    LINEAR {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = x
    },
    IN_SINE {
        override val opposite: Easing get() = OUT_SINE
        override fun inc0(x: Float): Float = 1.0f - cos(x * PI.toFloat() / 2.0f)
    },
    OUT_SINE {
        override val opposite: Easing get() = IN_SINE
        override fun inc0(x: Float): Float = sin(x * PI.toFloat() / 2.0f)
    },
    IN_OUT_SINE {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = -(cos(PI.toFloat() * x) - 1.0f) / 2.0f
    },
    IN_QUAD {
        override val opposite: Easing get() = OUT_QUAD
        override fun inc0(x: Float): Float = x * x
    },
    OUT_QUAD {
        override val opposite: Easing get() = IN_QUAD
        override fun inc0(x: Float): Float = 1.0f - (1.0f - x) * (1.0f - x)
    },
    IN_OUT_QUAD {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = if (x < 0.5f) 2.0f * x * x else 1.0f - (-2.0f * x + 2.0f).let { it * it } / 2.0f
    },
    IN_CUBIC {
        override val opposite: Easing get() = OUT_CUBIC
        override fun inc0(x: Float): Float = x * x * x
    },
    OUT_CUBIC {
        override val opposite: Easing get() = IN_CUBIC
        override fun inc0(x: Float): Float = 1.0f - (1.0f - x).let { it * it * it }
    },
    IN_OUT_CUBIC {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = if (x < 0.5f) 4.0f * x * x * x else 1.0f - (-2.0f * x + 2.0f).let { it * it * it } / 2.0f
    },
    IN_QUART {
        override val opposite: Easing get() = OUT_QUART
        override fun inc0(x: Float): Float = x * x * x * x
    },
    OUT_QUART {
        override val opposite: Easing get() = IN_QUART
        override fun inc0(x: Float): Float = 1.0f - (1.0f - x).let { it * it * it * it }
    },
    IN_OUT_QUART {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = if (x < 0.5f) 8.0f * x * x * x * x else 1.0f - (-2.0f * x + 2.0f).let { it * it * it * it } / 2.0f
    },
    IN_QUINT {
        override val opposite: Easing get() = OUT_QUINT
        override fun inc0(x: Float): Float = x * x * x * x * x
    },
    OUT_QUINT {
        override val opposite: Easing get() = IN_QUINT
        override fun inc0(x: Float): Float = 1.0f - (1.0f - x).let { it * it * it * it * it }
    },
    IN_OUT_QUINT {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = if (x < 0.5f) 16.0f * x * x * x * x * x else 1.0f - (-2.0f * x + 2.0f).let { it * it * it * it * it } / 2.0f
    },
    IN_EXPO {
        override val opposite: Easing get() = OUT_EXPO
        override fun inc0(x: Float): Float = if (x == 0.0f) 0.0f else 2.0f.pow(10.0f * x - 10.0f)
    },
    OUT_EXPO {
        override val opposite: Easing get() = IN_EXPO
        override fun inc0(x: Float): Float = if (x == 1.0f) 1.0f else 1.0f - 2.0f.pow(-10.0f * x)
    },
    IN_OUT_EXPO {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = when {
            x == 0.0f -> 0.0f
            x == 1.0f -> 1.0f
            x < 0.5f -> 2.0f.pow(20.0f * x - 10.0f) / 2.0f
            else -> (2.0f - 2.0f.pow(-20.0f * x + 10.0f)) / 2.0f
        }
    },
    IN_CIRC {
        override val opposite: Easing get() = OUT_CIRC
        override fun inc0(x: Float): Float = 1.0f - sqrt(1.0f - x * x)
    },
    OUT_CIRC {
        override val opposite: Easing get() = IN_CIRC
        override fun inc0(x: Float): Float = sqrt(1.0f - (x - 1.0f) * (x - 1.0f))
    },
    IN_OUT_CIRC {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = if (x < 0.5f) (1.0f - sqrt(1.0f - (2.0f * x) * (2.0f * x))) / 2.0f else (sqrt(1.0f - (-2.0f * x + 2.0f) * (-2.0f * x + 2.0f)) + 1.0f) / 2.0f
    },
    IN_BACK {
        override val opposite: Easing get() = OUT_BACK
        override fun inc0(x: Float): Float = 2.70158f * x * x * x - 1.70158f * x * x
    },
    OUT_BACK {
        override val opposite: Easing get() = IN_BACK
        override fun inc0(x: Float): Float = 1.0f + 2.70158f * (x - 1.0f).let { it * it * it } + 1.70158f * (x - 1.0f).let { it * it }
    },
    IN_OUT_BACK {
        override val opposite: Easing get() = this
        override fun inc0(x: Float): Float = if (x < 0.5f) (2.0f * x).let { it * it } * (7.189819f * x - 2.5949094f) / 2.0f else ((2.0f * x - 2.0f).let { it * it } * (3.5949094f * (x * 2.0f - 2.0f) + 2.5949094f) + 2.0f) / 2.0f
    };

    abstract val opposite: Easing

    protected abstract fun inc0(x: Float): Float

    fun dec0(x: Float): Float = 1.0f - inc0(x)

    fun inc(x: Float): Float = when {
        x <= 0.0f -> 0.0f
        x >= 1.0f -> 1.0f
        else -> inc0(x)
    }

    fun inc(x: Float, max: Float): Float = when {
        max == 0.0f -> 0.0f
        x <= 0.0f -> 0.0f
        x >= 1.0f -> max
        else -> inc0(x) * max
    }

    fun inc(x: Float, min: Float, max: Float): Float {
        var min2 = min
        var max2 = max
        if (max2 == min2) return 0.0f
        if (max2 < min2) {
            val oldMax = max2
            max2 = min2
            min2 = oldMax
        }
        return when {
            x <= 0.0f -> min2
            x >= 1.0f -> max2
            else -> MathUtils.lerp(min2, max2, inc0(x))
        }
    }

    fun dec(x: Float): Float = when {
        x <= 0.0f -> 1.0f
        x >= 1.0f -> 0.0f
        else -> dec0(x)
    }

    fun dec(x: Float, max: Float): Float = when {
        max == 0.0f -> 0.0f
        x <= 0.0f -> max
        x >= 1.0f -> 0.0f
        else -> dec0(x) * max
    }

    fun dec(x: Float, min: Float, max: Float): Float {
        var min2 = min
        var max2 = max
        if (max2 == min2) return 0.0f
        if (max2 < min2) {
            val oldMax = max2
            max2 = min2
            min2 = oldMax
        }
        return when {
            x <= 0.0f -> max2
            x >= 1.0f -> min2
            else -> MathUtils.lerp(min2, max2, dec0(x))
        }
    }

    fun incOrDec(x: Float, min: Float, max: Float): Float = MathUtils.lerp(min, max, inc(x))

    fun incOrDecOpposite(x: Float, min: Float, max: Float): Float {
        if (max == min) return min
        val delta = if (max > min) inc(x) else opposite.inc(x)
        return MathUtils.lerp(min, max, delta)
    }

    companion object {
        @JvmStatic
        fun toDelta(start: Long): Long = System.currentTimeMillis() - start

        @JvmStatic
        fun toDelta(start: Long, length: Float): Float = (toDelta(start).toFloat() / length).coerceIn(0.0f, 1.0f)

        @JvmStatic
        fun toDelta(start: Long, length: Long): Float = toDelta(start, length.toFloat())

        @JvmStatic
        fun toDelta(start: Long, length: Int): Float = toDelta(start, length.toFloat())
    }
}