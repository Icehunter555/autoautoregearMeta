package dev.wizard.meta.graphics.font.renderer

import dev.wizard.meta.graphics.font.glyph.FontGlyphs

class ExtendedFontRenderContext(override val fontRenderer: ExtendedFontRenderer) : AbstractFontRenderContext() {
    override var variant: FontGlyphs = fontRenderer.regularGlyph
        private set

    override fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean {
        if (index > 0 && text[index - 1] == '§') {
            return true
        }
        if (index < text.length - 1 && text[index] == '§') {
            val c = text[index + 1]
            if (rendering) {
                when (c) {
                    '0' -> color = 0
                    '1' -> color = 1
                    '2' -> color = 2
                    '3' -> color = 3
                    '4' -> color = 4
                    '5' -> color = 5
                    '6' -> color = 6
                    '7' -> color = 7
                    '8' -> color = 8
                    '9' -> color = 9
                    'a' -> color = 10
                    'b' -> color = 11
                    'c' -> color = 12
                    'd' -> color = 13
                    'e' -> color = 14
                    'f' -> color = 15
                    'l' -> variant = fontRenderer.boldGlyph
                    'o' -> variant = fontRenderer.italicGlyph
                    'r' -> {
                        variant = fontRenderer.regularGlyph
                        color = -1
                    }
                }
            } else {
                when (c) {
                    'r' -> variant = fontRenderer.regularGlyph
                    'l' -> variant = fontRenderer.boldGlyph
                    'o' -> variant = fontRenderer.italicGlyph
                }
            }
            return true
        }
        return false
    }
}
