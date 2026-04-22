package dev.wizard.meta.module.modules.client

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.graphics.texture.MipmapTexture
import dev.wizard.meta.manager.managers.EmojiManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

object Emoji : Module(
    "Emoji",
    category = Category.CLIENT,
    description = "Add emojis to chat."
) {
    private val regex = Regex(":(.+?):")

    @JvmStatic
    fun renderText(inputText: String, fontHeight: Int, shadow: Boolean, posX: Float, posY: Float, alpha: Float): String {
        var text = inputText
        val blend = GL11.glGetBoolean(GL11.GL_BLEND)
        val replacement = getReplacement(fontHeight)

        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha)
        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 1)

        regex.findAll(inputText).forEach { result ->
            val emojiName = result.groupValues.getOrNull(1) ?: return@forEach
            val emojiTexture = EmojiManager.getEmoji(emojiName) ?: return@forEach
            val emojiText = result.value

            if (!shadow) {
                val index = text.indexOf(emojiText)
                if (index != -1) {
                    val beforeEmoji = text.substring(0, index)
                    val x = getStringWidth(beforeEmoji) + fontHeight / 4
                    drawEmoji(emojiTexture, (posX + x).toDouble(), posY.toDouble(), fontHeight.toFloat())
                }
            }
            text = text.replaceFirst(emojiText, replacement)
        }

        GlStateUtils.blend(blend)
        GlStateManager.tryBlendFuncSeparate(770, 771, 770, 771)
        return text
    }

    @JvmStatic
    fun getStringWidthCustomFont(inputText: String): Int {
        var text = inputText
        val replacement = getReplacementCustomFont()
        regex.findAll(inputText).forEach { result ->
            val emojiName = result.groupValues.getOrNull(1) ?: return@forEach
            if (!EmojiManager.isEmoji(emojiName)) return@forEach
            val emojiText = result.value
            text = text.replaceFirst(emojiText, replacement)
        }
        return MathUtilKt.ceilToInt(MainFontRenderer.getWidth(text).toFloat())
    }

    @JvmStatic
    fun getStringWidth(inputWidth: Int, inputText: String, fontHeight: Int): Int {
        var reducedWidth = inputWidth
        val replacementWidth = getStringWidth(getReplacement(fontHeight))
        regex.findAll(inputText).forEach { result ->
            val emojiName = result.groupValues.getOrNull(1) ?: return@forEach
            if (!EmojiManager.isEmoji(emojiName)) return@forEach
            val emojiText = result.value
            val emojiTextWidth = getStringWidth(emojiText)
            reducedWidth -= (emojiTextWidth - replacementWidth)
        }
        return reducedWidth
    }

    private fun getReplacementCustomFont(): String {
        val emojiWidth = MathUtilKt.ceilToInt(MainFontRenderer.height / MainFontRenderer.getWidth(' '))
        return " ".repeat(emojiWidth)
    }

    private fun getReplacement(fontHeight: Int): String {
        val emojiWidth = MathUtilKt.ceilToInt(fontHeight.toFloat() / mc.fontRenderer.getCharWidth(' '))
        return " ".repeat(emojiWidth)
    }

    fun getStringWidth(text: String): Int {
        if (CustomFont.isEnabled && CustomFont.overrideMinecraft) {
            return getStringWidthCustomFont(text)
        }
        var i = 0
        var flag = false
        var j = 0
        while (j < text.length) {
            var c0 = text[j]
            var k = mc.fontRenderer.getCharWidth(c0)
            if (k < 0 && j < text.length - 1) {
                c0 = text[++j]
                if (c0 != 'l' && c0 != 'L') {
                    if (c0 == 'r' || c0 == 'R') {
                        flag = false
                    }
                } else {
                    flag = true
                }
                k = 0
            }
            i += k
            if (flag && k > 0) {
                i++
            }
            j++
        }
        return i
    }

    private fun drawEmoji(texture: MipmapTexture, x: Double, y: Double, size: Float) {
        val tessellator = Tessellator.getInstance()
        val bufBuilder = tessellator.buffer
        texture.bindTexture()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        bufBuilder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX)
        bufBuilder.pos(x, y + size.toDouble(), 0.0).tex(0.0, 1.0).endVertex()
        bufBuilder.pos(x + size.toDouble(), y + size.toDouble(), 0.0).tex(1.0, 1.0).endVertex()
        bufBuilder.pos(x, y, 0.0).tex(0.0, 0.0).endVertex()
        bufBuilder.pos(x + size.toDouble(), y, 0.0).tex(1.0, 0.0).endVertex()
        tessellator.draw()
    }
}
