package dev.wizard.meta.graphics.color

data class ColorHSB(val h: Float, val s: Float, val b: Float, val a: Float = 1.0f) {
    fun toRGB(): ColorRGB = ColorUtils.hsbToRGB(h, s, b, a)

    override fun toString(): String = "$h, $s, $b, $a"
}