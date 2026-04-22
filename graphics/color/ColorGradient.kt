package dev.wizard.meta.graphics.color

import dev.wizard.meta.util.math.MathUtils
import kotlin.math.roundToInt

class ColorGradient(vararg stops: Stop) {
    private val colorArray: Array<out Stop> = stops.apply {
        if (size > 1) {
            sortBy { it.value }
        }
    }

    fun get(valueIn: Float): Int {
        if (colorArray.isEmpty()) {
            return ColorRGB(255, 255, 255)
        }
        var prevStop = colorArray.last()
        var nextStop = colorArray.last()

        for ((index, pair) in colorArray.withIndex()) {
            if (pair.value >= valueIn) {
                prevStop = if (pair.value == valueIn) pair else colorArray[maxOf(index - 1, 0)]
                nextStop = pair
                break
            }
        }

        if (prevStop == nextStop) {
            return prevStop.color
        }

        val r = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, ColorRGB.getR(prevStop.color).toFloat(), ColorRGB.getR(nextStop.color).toFloat()).roundToInt()
        val g = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, ColorRGB.getG(prevStop.color).toFloat(), ColorRGB.getG(nextStop.color).toFloat()).roundToInt()
        val b = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, ColorRGB.getB(prevStop.color).toFloat(), ColorRGB.getB(nextStop.color).toFloat()).roundToInt()
        val a = MathUtils.convertRange(valueIn, prevStop.value, nextStop.value, ColorRGB.getA(prevStop.color).toFloat(), ColorRGB.getA(nextStop.color).toFloat()).roundToInt()

        return ColorRGB(r, g, b, a)
    }

    class Stop(val value: Float, val color: Int)
}
