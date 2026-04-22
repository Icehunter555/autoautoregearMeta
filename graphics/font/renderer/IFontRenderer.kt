package dev.wizard.meta.graphics.font.renderer

import dev.wizard.meta.graphics.color.ColorRGB

interface IFontRenderer {
    fun drawString(
        charSequence: CharSequence,
        posX: Float = 0.0f,
        posY: Float = 0.0f,
        color: ColorRGB = ColorRGB(255, 255, 255),
        scale: Float = 1.0f,
        drawShadow: Boolean = true
    )

    fun getHeight(): Float = getHeight(1.0f)

    fun getHeight(scale: Float): Float

    fun getWidth(text: CharSequence): Float = getWidth(text, 1.0f)

    fun getWidth(charSequence: CharSequence, scale: Float): Float

    fun getWidth(char: Char): Float = getWidth(char, 1.0f)

    fun getWidth(char: Char, scale: Float): Float
}
