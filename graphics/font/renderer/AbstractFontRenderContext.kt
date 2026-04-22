package dev.wizard.meta.graphics.font.renderer

import dev.wizard.meta.graphics.font.glyph.FontGlyphs

abstract class AbstractFontRenderContext {
    var color: Int = -1
        protected set

    protected abstract val fontRenderer: IFontRenderer

    abstract val variant: FontGlyphs

    abstract fun checkFormatCode(text: CharSequence, index: Int, rendering: Boolean): Boolean
}
