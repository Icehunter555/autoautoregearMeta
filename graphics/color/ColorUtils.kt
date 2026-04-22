package dev.wizard.meta.graphics.color

import java.awt.Color
import kotlin.math.max
import kotlin.math.min

object ColorUtils {
    private const val ONE_THIRD = 0.33333334f
    private const val TWO_THIRD = 0.6666667f

    fun getRed(hex: Int): Int = hex shr 16 and 0xFF
    fun getGreen(hex: Int): Int = hex shr 8 and 0xFF
    fun getBlue(hex: Int): Int = hex and 0xFF

    fun getHoovered(color: Int, isHoovered: Boolean): Int {
        return if (isHoovered) (color and 0x7F7F7F) shl 1 else color
    }

    fun getHoovered(color: Color, isHoovered: Boolean): Int {
        val rgb = color.rgb
        return if (isHoovered) (rgb and 0x7F7F7F) shl 1 else rgb
    }

    fun argbToRgba(argb: Int): Int {
        return (argb and 0xFFFFFF) shl 8 or (argb shr 24 and 0xFF)
    }

    fun rgbaToArgb(rgba: Int): Int {
        return (rgba shr 8 and 0xFFFFFF) or ((rgba and 0xFF) shl 24)
    }

    fun rgbToHSB(r: Int, g: Int, b: Int, a: Int): ColorHSB {
        val cMax = max(r, max(g, b))
        if (cMax == 0) return ColorHSB(0.0f, 0.0f, 0.0f, a / 255.0f)
        val cMin = min(r, min(g, b))
        val diff = cMax - cMin
        val diff6 = diff.toFloat() * 6.0f
        var hue = when (cMax) {
            cMin -> 0.0f
            r -> (g - b).toFloat() / diff6 + 1.0f
            g -> (b - r).toFloat() / diff6 + ONE_THIRD
            else -> (r - g).toFloat() / diff6 + TWO_THIRD
        }
        val saturation = diff.toFloat() / cMax.toFloat()
        val brightness = cMax.toFloat() / 255.0f
        hue %= 1.0f
        return ColorHSB(hue, saturation, brightness, a / 255.0f)
    }

    fun rgbToHue(r: Int, g: Int, b: Int): Float {
        val cMax = max(r, max(g, b))
        if (cMax == 0) return 0.0f
        val cMin = min(r, min(g, b))
        if (cMax == cMin) return 0.0f
        val diff = (cMax - cMin).toFloat() * 6.0f
        val hue = when (cMax) {
            r -> (g - b).toFloat() / diff + 1.0f
            g -> (b - r).toFloat() / diff + ONE_THIRD
            else -> (r - g).toFloat() / diff + TWO_THIRD
        }
        return hue % 1.0f
    }

    fun rgbToSaturation(r: Int, g: Int, b: Int): Float {
        val cMax = max(r, max(g, b))
        if (cMax == 0) return 0.0f
        val cMin = min(r, min(g, b))
        return (cMax - cMin).toFloat() / cMax.toFloat()
    }

    fun rgbToBrightness(r: Int, g: Int, b: Int): Float {
        return max(r, max(g, b)).toFloat() / 255.0f
    }

    fun rgbToLightness(r: Int, g: Int, b: Int): Float {
        return (max(r, max(g, b)) + min(r, min(g, b))).toFloat() / 510.0f
    }

    fun hsbToRGB(h: Float, s: Float, b: Float, a: Float = 1.0f): ColorRGB {
        val hue6 = h % 1.0f * 6.0f
        val intHue6 = hue6.toInt()
        val f = hue6 - intHue6
        val p = b * (1.0f - s)
        val q = b * (1.0f - f * s)
        val t = b * (1.0f - (1.0f - f) * s)
        return when (intHue6) {
            0 -> ColorRGB(b, t, p, a)
            1 -> ColorRGB(q, b, p, a)
            2 -> ColorRGB(p, b, t, a)
            3 -> ColorRGB(p, q, b, a)
            4 -> ColorRGB(t, p, b, a)
            5 -> ColorRGB(b, p, q, a)
            else -> ColorRGB(1.0f, 1.0f, 1.0f, a)
        }
    }

    fun Color.toRGB(): ColorRGB {
        return ColorRGB(argbToRgba(this.rgb))
    }

    fun Color.toHSB(): ColorHSB {
        return ColorRGB(this.red, this.green, this.blue, this.alpha).toHSB()
    }
}