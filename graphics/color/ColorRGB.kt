package dev.wizard.meta.graphics.color

import java.util.*
import kotlin.math.*

@JvmInline
value class ColorRGB(val rgba: Int) {
    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        (r and 0xFF shl 24) or (g and 0xFF shl 16) or (b and 0xFF shl 8) or (a and 0xFF)
    )

    constructor(r: Float, g: Float, b: Float, a: Float = 1.0f) : this(
        (r * 255.0f).toInt(), (g * 255.0f).toInt(), (b * 255.0f).toInt(), (a * 255.0f).toInt()
    )

    val r: Int get() = rgba shr 24 and 0xFF
    val g: Int get() = rgba shr 16 and 0xFF
    val b: Int get() = rgba shr 8 and 0xFF
    val a: Int get() = rgba and 0xFF

    val rFloat: Float get() = r / 255.0f
    val gFloat: Float get() = g / 255.0f
    val bFloat: Float get() = b / 255.0f
    val aFloat: Float get() = a / 255.0f

    fun getHue(): Float = ColorUtils.rgbToHue(r, g, b)
    fun getSaturation(): Float = ColorUtils.rgbToSaturation(r, g, b)
    fun getBrightness(): Float = ColorUtils.rgbToBrightness(r, g, b)
    fun getLightness(): Float = ColorUtils.rgbToLightness(r, g, b)

    fun withRed(r: Int): ColorRGB = ColorRGB(rgba and 0x00FFFFFF or (r and 0xFF shl 24))
    fun withGreen(g: Int): ColorRGB = ColorRGB(rgba and 0xFF00FFFF.toInt() or (g and 0xFF shl 16))
    fun withBlue(b: Int): ColorRGB = ColorRGB(rgba and 0xFFFF00FF.toInt() or (b and 0xFF shl 8))
    fun withAlpha(a: Int): ColorRGB = ColorRGB(rgba and 0xFFFFFF00.toInt() or (a and 0xFF))

    fun mix(other: ColorRGB, ratio: Float): ColorRGB {
        val ratioSelf = 1.0f - ratio
        return ColorRGB(
            (r * ratioSelf + other.r * ratio).toInt(),
            (g * ratioSelf + other.g * ratio).toInt(),
            (b * ratioSelf + other.b * ratio).toInt(),
            (a * ratioSelf + other.a * ratio).toInt()
        )
    }

    fun mix(other: ColorRGB): ColorRGB {
        return ColorRGB(
            (r + other.r) / 2,
            (g + other.g) / 2,
            (b + other.b) / 2,
            (a + other.a) / 2
        )
    }

    fun darker(factor: Float): ColorRGB {
        return ColorRGB(
            (rFloat * (1.0f - factor)).coerceIn(0f, 1f),
            (gFloat * (1.0f - factor)).coerceIn(0f, 1f),
            (bFloat * (1.0f - factor)).coerceIn(0f, 1f),
            aFloat
        )
    }

    fun toArgb(): Int = ColorUtils.rgbaToArgb(rgba)

    fun toHSB(): ColorHSB = ColorUtils.rgbToHSB(r, g, b, a)

    override fun toString(): String = "$r, $g, $b, $a"

    companion object {
        fun lerp(start: ColorRGB, end: ColorRGB, progress: Float): ColorRGB {
            val invProgress = 1.0f - progress
            return ColorRGB(
                (start.rFloat * invProgress + end.rFloat * progress).coerceIn(0f, 1f),
                (start.gFloat * invProgress + end.gFloat * progress).coerceIn(0f, 1f),
                (start.bFloat * invProgress + end.bFloat * progress).coerceIn(0f, 1f),
                (start.aFloat * invProgress + end.aFloat * progress).coerceIn(0f, 1f)
            )
        }

        fun multiLerp(colors: List<ColorRGB>, progress: Float): ColorRGB {
            if (colors.isEmpty()) return ColorRGB(255, 255, 255)
            if (colors.size == 1) return colors[0]
            val segmentSize = 1.0f / (colors.size - 1)
            val segment = (progress / segmentSize).toInt().coerceAtMost(colors.size - 2)
            val segmentProgress = (progress - segment * segmentSize) / segmentSize
            return lerp(colors[segment], colors[segment + 1], segmentProgress)
        }

        fun fromHSB(h: Float, s: Float, b: Float, a: Float = 1.0f): ColorRGB {
            return ColorUtils.hsbToRGB(h, s, b, a)
        }
    }
}