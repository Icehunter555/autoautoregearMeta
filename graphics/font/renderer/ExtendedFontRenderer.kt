package dev.wizard.meta.graphics.font.renderer

import dev.wizard.meta.graphics.font.Style
import dev.wizard.meta.graphics.font.glyph.FontGlyphs
import java.awt.Font

open class ExtendedFontRenderer(font: Font, size: Float, textureSize: Int) : AbstractFontRenderer(font, size, textureSize) {
    val boldGlyph: FontGlyphs = loadFont(font, size, Style.BOLD)
    val italicGlyph: FontGlyphs = loadFont(font, size, Style.ITALIC)

    override fun getRenderContext(): ExtendedFontRenderContext {
        return ExtendedFontRenderContext(this)
    }

    override fun destroy() {
        super.destroy()
        boldGlyph.destroy()
        italicGlyph.destroy()
    }
}
