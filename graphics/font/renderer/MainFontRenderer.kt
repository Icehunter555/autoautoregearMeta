package dev.wizard.meta.graphics.font.renderer

import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.color.ColorUtils
import dev.wizard.meta.module.modules.client.CustomFont
import org.lwjgl.opengl.GL20
import java.awt.Font
import java.io.InputStream

object MainFontRenderer : IFontRenderer {
    private var delegate: ExtendedFontRenderer
    private val defaultFont: Font

    init {
        val stream = this::class.java.getResourceAsStream("/assets/meta/fonts/Orbitron.ttf")!!
        defaultFont = Font.createFont(Font.TRUETYPE_FONT, stream)
        stream.close()
        delegate = loadFont()
    }

    fun reloadFonts() {
        delegate.destroy()
        delegate = loadFont()
    }

    private fun loadFont(): ExtendedFontRenderer {
        val font = try {
            val fontToUse = CustomFont.INSTANCE.fontToUse.value
            val fontName = when (fontToUse) {
                CustomFont.FontToUse.QUEEN -> "Queen.ttf"
                CustomFont.FontToUse.GEO -> "Geo.ttf"
                CustomFont.FontToUse.COMIC -> "Comic.ttf"
                CustomFont.FontToUse.UNDERDOG -> "Underdog.ttf"
                CustomFont.FontToUse.WINKYSANS -> "WinkySans.ttf"
                CustomFont.FontToUse.GIDOLE -> "Gidole.ttf"
                CustomFont.FontToUse.JETBRAINS -> "Jetbrains.ttf"
                CustomFont.FontToUse.MONOFUR -> "Monofur.ttf"
                CustomFont.FontToUse.GOLDMAN -> "Goldman.ttf"
                CustomFont.FontToUse.LEXEND -> "Lexend.ttf"
                CustomFont.FontToUse.MACONDO -> "Macondo.ttf"
                CustomFont.FontToUse.STORY -> "Story.ttf"
                CustomFont.FontToUse.TEKTUR -> "Tektur.ttf"
                CustomFont.FontToUse.MINECRAFT -> "Minecraft.ttf"
                CustomFont.FontToUse.GHRATHE -> "Ghrathe.ttf"
                CustomFont.FontToUse.FOR3UER -> "3uer.ttf"
                else -> null
            }
            if (fontName != null) {
                val stream = this::class.java.getResourceAsStream("/assets/meta/fonts/$fontName")!!
                val f = Font.createFont(Font.TRUETYPE_FONT, stream)
                stream.close()
                f
            } else {
                defaultFont
            }
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed loading main font. Using default Orbitron font.", e)
            defaultFont
        }
        return DelegateFontRenderer(font)
    }

    fun drawStringJava(string: String, posX: Float, posY: Float, color: Int, scale: Float, drawShadow: Boolean) {
        var adjustedColor = color
        if ((adjustedColor and -0x4000000) == 0) {
            adjustedColor = color or -0x1000000
        }
        GlStateUtils.alpha(false)
        drawString(string, posX, posY - 1.0f, ColorRGB(ColorUtils.argbToRgba(adjustedColor)), scale, drawShadow)
        GlStateUtils.alpha(true)
        GL20.glUseProgram(0)
    }

    override fun drawString(
        charSequence: CharSequence,
        posX: Float,
        posY: Float,
        color: ColorRGB,
        scale: Float,
        drawShadow: Boolean
    ) {
        delegate.drawString(charSequence, posX, posY, color, scale, drawShadow)
    }

    override fun getWidth(text: CharSequence, scale: Float): Float {
        return delegate.getWidth(text, scale)
    }

    override fun getWidth(c: Char, scale: Float): Float {
        return delegate.getWidth(c, scale)
    }

    override fun getHeight(scale: Float): Float {
        return delegate.regularGlyph.fontHeight * CustomFont.INSTANCE.lineSpace * scale
    }

    private class DelegateFontRenderer(font: Font) : ExtendedFontRenderer(font, 64.0f, 2048) {
        override fun getSizeMultiplier(): Float = CustomFont.INSTANCE.size
        override fun getBaselineOffset(): Float = CustomFont.INSTANCE.baselineOffset
        override fun getCharGap(): Float = CustomFont.INSTANCE.charGap
        override fun getLineSpace(): Float = CustomFont.INSTANCE.lineSpace
        override fun getLodBias(): Float = CustomFont.INSTANCE.lodBias
        override fun getShadowDist(): Float = 5.0f
    }
}
