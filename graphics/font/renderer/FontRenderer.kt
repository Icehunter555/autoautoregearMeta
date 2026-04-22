package dev.wizard.meta.graphics.font.renderer

import java.awt.Font

open class FontRenderer(font: Font, size: Float, textureSize: Int) : AbstractFontRenderer(font, size, textureSize) {
    override fun getRenderContext(): FontRenderContext {
        return FontRenderContext(this)
    }
}
