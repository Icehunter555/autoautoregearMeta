package dev.wizard.meta.util

import net.minecraft.client.renderer.GlStateManager
import java.util.regex.Pattern

object RGBColorProcessor {
    private val RGB_PATTERN: Pattern = Pattern.compile("§x§([0-9a-fA-F]{2})§([0-9a-fA-F]{2})§([0-9a-fA-F]{2})")

    @JvmStatic
    fun processRGBText(text: String?): String {
        if (text.isNullOrEmpty()) return text ?: ""
        val matcher = RGB_PATTERN.matcher(text)
        val result = StringBuffer()
        while (matcher.find()) {
            try {
                val red = matcher.group(1).toInt(16)
                val green = matcher.group(2).toInt(16)
                val blue = matcher.group(3).toInt(16)
                GlStateManager.color(red / 255f, green / 255f, blue / 255f, 1.0f)
                matcher.appendReplacement(result, "")
            } catch (e: NumberFormatException) {
                matcher.appendReplacement(result, matcher.group())
            }
        }
        matcher.appendTail(result)
        return result.toString()
    }

    @JvmStatic
    fun stripRGBCodes(text: String?): String {
        if (text.isNullOrEmpty()) return text ?: ""
        return RGB_PATTERN.matcher(text).replaceAll("")
    }

    @JvmStatic
    fun hasRGBCodes(text: String?): Boolean {
        return text != null && RGB_PATTERN.matcher(text).find()
    }
}
