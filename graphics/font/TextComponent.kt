package dev.wizard.meta.graphics.font

import dev.wizard.meta.graphics.HAlign
import dev.wizard.meta.graphics.VAlign
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.IFontRenderer
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.util.math.vector.Vec2d
import net.minecraft.client.renderer.GlStateManager
import java.util.*
import kotlin.math.max

class TextComponent(val separator: String = " ") {
    private val textLines = ArrayList<TextLine?>()
    var currentLine: Int = 0
        set(value) {
            field = max(value, 0)
        }

    constructor(textComponent: TextComponent) : this(textComponent.separator) {
        textLines.addAll(textComponent.textLines)
        currentLine = textComponent.currentLine
    }

    constructor(string: String, separator: String = " ", vararg delimiters: String) : this(separator) {
        val lines = string.lines()
        lines.forEachIndexed { index, line ->
            line.split(*delimiters).forEach { splitText ->
                add(splitText)
            }
            if (index != lines.lastIndex) {
                currentLine++
            }
        }
    }

    fun addLine(text: String, color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        add(text, color, style)
        currentLine++
    }

    fun addLine(textElement: TextElement) {
        add(textElement)
        currentLine++
    }

    fun add(text: String, color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        add(TextElement(text, color, style))
    }

    fun addNoSeparator(text: String, color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        addNoSeparator(TextElement(text, color, style))
    }

    fun add(textElement: TextElement) {
        while (textLines.size <= currentLine) {
            textLines.add(null)
        }
        val line = textLines[currentLine] ?: TextLine(separator).also { textLines[currentLine] = it }
        line.add(textElement)
    }

    fun addNoSeparator(textElement: TextElement) {
        while (textLines.size <= currentLine) {
            textLines.add(null)
        }
        val line = textLines[currentLine] ?: TextLine(separator).also { textLines[currentLine] = it }
        line.addNoSeparator(textElement)
    }

    fun clear() {
        textLines.clear()
        currentLine = 0
    }

    fun draw(
        pos: Vec2d = Vec2d.ZERO,
        lineSpace: Int = 2,
        alpha: Float = 1.0f,
        scale: Float = 1.0f,
        skipEmptyLine: Boolean = true,
        horizontalAlign: HAlign = HAlign.LEFT,
        verticalAlign: VAlign = VAlign.TOP
    ) {
        if (isEmpty()) return
        GlStateManager.func_179094_E()
        GlStateManager.func_179137_b(pos.x, pos.y - 1.0, 0.0)
        GlStateManager.func_179152_a(scale, scale, 1.0f)
        if (verticalAlign != VAlign.TOP) {
            var height = getHeight(lineSpace, skipEmptyLine)
            if (verticalAlign == VAlign.CENTER) height /= 2f
            GlStateManager.func_179109_b(0.0f, -height, 0.0f)
        }
        textLines.forEach { line ->
            if (skipEmptyLine && (line == null || line.isEmpty())) return@forEach
            line?.drawLine(alpha, horizontalAlign)
            GlStateManager.func_179109_b(0.0f, MainFontRenderer.height + lineSpace, 0.0f)
        }
        GlStateManager.func_179121_F()
    }

    fun isEmpty(): Boolean {
        return textLines.none { it != null && !it.isEmpty() }
    }

    val width: Float
        get() = textLines.maxOfOrNull { it?.width ?: 0.0f } ?: 0.0f

    fun getHeight(lineSpace: Int, skipEmptyLines: Boolean = true): Float {
        val count = getLines(skipEmptyLines)
        return MainFontRenderer.height * count + (lineSpace * (count - 1))
    }

    fun getLines(skipEmptyLines: Boolean = true): Int {
        return textLines.count { !skipEmptyLines || (it != null && !it.isEmpty()) }
    }

    override fun toString(): String {
        return textLines.joinToString("\n")
    }

    class TextElement(textIn: String, val color: ColorRGB = ColorRGB(255, 255, 255), style: Style = Style.REGULAR) {
        val text: String = style.code + textIn

        override fun toString(): String = text
    }

    class TextLine(private val separator: String) {
        private val textElementList = ArrayList<TextElementData>()
        private var cachedString: String = ""

        fun isEmpty(): Boolean = textElementList.isEmpty()

        fun add(textElement: TextElement) {
            textElementList.add(TextElementData(textElement, true))
            updateCache()
        }

        fun addNoSeparator(textElement: TextElement) {
            textElementList.add(TextElementData(textElement, false))
            updateCache()
        }

        fun drawLine(alpha: Float, horizontalAlign: HAlign) {
            GlStateManager.func_179094_E()
            if (horizontalAlign != HAlign.LEFT) {
                var w = getWidth()
                if (horizontalAlign == HAlign.CENTER) w /= 2f
                GlStateManager.func_179109_b(-w, 0.0f, 0.0f)
            }
            textElementList.forEachIndexed {
                index, elementData ->
                val textElement = elementData.element
                val color = textElement.color.alpha((textElement.color.a * alpha).toInt())
                MainFontRenderer.drawString(textElement.text, 0.0f, 0.0f, color)
                val elementWidth = MainFontRenderer.getWidth(textElement.text)
                GlStateManager.func_179109_b(elementWidth, 0.0f, 0.0f)
                if (elementData.hasSeparator && index < textElementList.size - 1) {
                    val adjustedSeparator = if (separator == " ") "  " else separator
                    val separatorWidth = MainFontRenderer.getWidth(adjustedSeparator)
                    GlStateManager.func_179109_b(separatorWidth, 0.0f, 0.0f)
                }
            }
            GlStateManager.func_179121_F()
        }

        fun getWidth(): Float = MainFontRenderer.getWidth(cachedString)

        fun reverse() {
            textElementList.reverse()
            updateCache()
        }

        private fun updateCache() {
            val sb = StringBuilder()
            textElementList.forEachIndexed {
                index, elementData ->
                sb.append(elementData.element.text)
                if (elementData.hasSeparator && index < textElementList.size - 1) {
                    val adjustedSeparator = if (separator == " ") "  " else separator
                    sb.append(adjustedSeparator)
                }
            }
            cachedString = sb.toString()
        }

        private data class TextElementData(val element: TextElement, val hasSeparator: Boolean)
    }
}
