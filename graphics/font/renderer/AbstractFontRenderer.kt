package dev.wizard.meta.graphics.font.renderer

import dev.fastmc.common.TickTimer
import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.MatrixUtils
import dev.wizard.meta.graphics.font.RenderString
import dev.wizard.meta.graphics.font.Style
import dev.wizard.meta.graphics.font.glyph.FontGlyphs
import dev.wizard.meta.module.modules.client.CustomFont
import dev.wizard.meta.util.extension.synchronized
import dev.wizard.meta.util.threads.onMainThread
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.joml.Matrix4f
import java.awt.Font
import java.util.*
import dev.wizard.meta.graphics.color.ColorRGB

abstract class AbstractFontRenderer(font: Font, size: Float, private val textureSize: Int) : IFontRenderer {
    private val glyphs = ArrayList<FontGlyphs>()
    val regularGlyph: FontGlyphs
    private var prevCharGap = Float.NaN
    private var prevLineSpace = Float.NaN
    private var prevShadowDist = Float.NaN
    private val renderStringMap = Object2ObjectOpenHashMap<CharSequence, RenderString>().synchronized()
    private val checkTimer = TickTimer()
    private val cleanTimer = TickTimer()
    private val modelViewMatrix = Matrix4f()

    init {
        regularGlyph = loadFont(font, size, Style.REGULAR)
    }

    abstract fun getRenderContext(): AbstractFontRenderContext

    protected open fun getSizeMultiplier(): Float = 1.0f

    protected open fun getBaselineOffset(): Float = 0.0f

    override fun getCharGap(): Float = 0.0f

    protected open fun getLineSpace(): Float = 1.0f

    protected open fun getLodBias(): Float = 0.0f

    protected open fun getShadowDist(): Float = 2.0f

    protected fun loadFont(font: Font, size: Float, style: Style): FontGlyphs {
        val fallbackFont = try {
            Companion.fallbackFont.deriveFont(style.styleConst, size)
        } catch (e: Exception) {
            MetaMod.logger.warn("Failed loading fallback font. Using Sans Serif font", e)
            Companion.sansSerifFont.deriveFont(style.styleConst, size)
        }
        val styledFont = font.deriveFont(style.styleConst, size)
        val newGlyphs = FontGlyphs(style.ordinal, styledFont, fallbackFont, textureSize)
        glyphs.add(newGlyphs)
        return newGlyphs
    }

    override fun drawString(
        charSequence: CharSequence,
        posX: Float,
        posY: Float,
        color: ColorRGB,
        scale: Float,
        drawShadow: Boolean
    ) {
        if (checkTimer.tickAndReset(25L)) {
            if (glyphs.any { it.checkUpdate() }) {
                // Cache invalidated by glyph updates
            }
        }

        if (cleanTimer.tick(3000L)) {
            val current = System.currentTimeMillis()
            synchronized(renderStringMap) {
                renderStringMap.values.removeIf { it.tryClean(current) }
            }
            cleanTimer.reset()
        }

        if (prevCharGap != getCharGap() || prevLineSpace != getLineSpace() || prevShadowDist != getShadowDist()) {
            clearStringCache()
            prevCharGap = getCharGap()
            prevLineSpace = getLineSpace()
            prevShadowDist = getShadowDist()
        }

        val stringCache = renderStringMap.computeIfAbsent(charSequence.toString()) {
            RenderString(it, this).build(this, getCharGap(), getLineSpace(), getShadowDist())
        }

        GlStateUtils.texture2d(true)
        GlStateUtils.blend(true)

        val modelView = MatrixUtils.loadModelViewMatrix()
            .getMatrix(modelViewMatrix)
            .translate(posX, posY, 0.0f)
            .scale(getSizeMultiplier() * scale, getSizeMultiplier() * scale, 1.0f)
            .translate(0.0f, getBaselineOffset(), 0.0f)

        stringCache.render(modelView, color, drawShadow, getLodBias())
    }

    override fun getHeight(scale: Float): Float {
        return regularGlyph.fontHeight * getSizeMultiplier() * scale
    }

    override fun getWidth(text: CharSequence, scale: Float): Float {
        val string = text.toString()
        var renderString = renderStringMap[string]
        if (renderString == null) {
            val rs = RenderString(string, this)
            renderString = rs
            onMainThread {
                renderStringMap[string] = rs.build(this, getCharGap(), getLineSpace(), getShadowDist())
            }
        }
        return renderString.width * getSizeMultiplier() * scale
    }

    override fun getWidth(c: Char, scale: Float): Float {
        return (regularGlyph.getCharInfo(c).width + getCharGap()) * getSizeMultiplier() * scale
    }

    open fun destroy() {
        clearStringCache()
        regularGlyph.destroy()
    }

    fun clearStringCache() {
        renderStringMap.values.forEach { it.destroy() }
        renderStringMap.clear()
    }

    companion object {
        private val fallbackFonts = arrayOf(
            "microsoft yahei ui", "microsoft yahei", "noto sans jp", "noto sans cjk jp", "noto sans cjk jp",
            "noto sans cjk kr", "noto sans cjk sc", "noto sans cjk tc", "source han sans", "source han sans hc",
            "source han sans sc", "source han sans tc", "source han sans k"
        )

        val fallbackFont: Font
            get() {
                val name = fallbackFonts.firstOrNull { CustomFont.INSTANCE.availableFonts.contains(it) }
                return Font(name, 0, 64)
            }

        val sansSerifFont: Font
            get() = Font("SansSerif", 0, 64)
    }
}
