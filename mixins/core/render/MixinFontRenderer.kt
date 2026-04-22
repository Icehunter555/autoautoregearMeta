package dev.wizard.meta.mixins.core.render

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.MetaMod
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.modules.client.CustomFont
import dev.wizard.meta.module.modules.client.Emoji
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.module.modules.misc.AltProtect
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.text.TextFormatting
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.regex.Pattern

@Mixin(FontRenderer::class)
abstract class MixinFontRenderer {
    @Shadow
    var field_78288_b = 0

    @Shadow
    protected var field_78295_j = 0f

    @Shadow
    protected var field_78296_k = 0f

    @Shadow
    private var field_78291_n = 0f

    @Shadow
    private var field_78306_p = 0f

    @Shadow
    private var field_78292_o = 0f

    @Shadow
    private var field_78305_q = 0f

    @Shadow
    private var field_78304_r = 0

    @Shadow
    protected abstract fun func_78255_a(var1: String, var2: Boolean)

    @Shadow
    protected abstract fun func_78265_b()

    @Shadow
    protected abstract fun func_181559_a(var1: Char, var2: Boolean): Float

    @ModifyVariable(method = ["drawString(Ljava/lang/String;FFIZ)I"], at = At("HEAD"), ordinal = 0, argsOnly = true)
    private fun modifyDrawString(text: String): String {
        var textVar = text
        if (AltProtect.isEnabled && AltProtect.nameProtect) {
            textVar = textVar.replace(AltProtect.currentName, AltProtect.fakeName.value)
        }
        textVar = this.processNamedColors(textVar)
        return this.processHexColors(textVar)
    }

    @Inject(method = ["drawString(Ljava/lang/String;FFIZ)I"], at = [At("HEAD")], cancellable = true)
    private fun `drawString$Inject$HEAD`(text: String, x: Float, y: Float, color: Int, dropShadow: Boolean, cir: CallbackInfoReturnable<Int>) {
        this.trollHack$handleDrawString(text, x, y, color, dropShadow, cir)
    }

    @Unique
    private fun `trollHack$handleDrawString`(text: String, x: Float, y: Float, color: Int, drawShadow: Boolean, cir: CallbackInfoReturnable<Int>) {
        if (MetaMod.isReady() && CustomFont.overrideMinecraft) {
            this.field_78295_j = x
            this.field_78296_k = y
            var textVar = text
            if (Emoji.isEnabled && textVar.contains(":")) {
                textVar = Emoji.renderText(textVar, this.field_78288_b, false, this.field_78295_j, this.field_78296_k, this.field_78305_q)
                GlStateManager.color(this.field_78291_n, this.field_78292_o, this.field_78306_p, this.field_78305_q)
            }
            GlStateManager.tryBlendFuncSeparate(770, 771, 770, 771)
            MainFontRenderer.drawStringJava(textVar, x, y, color, 1.0f, drawShadow)
            cir.returnValue = MathUtilKt.ceilToInt(x + MainFontRenderer.getWidth(textVar))
        }
    }

    @Inject(method = ["renderString"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderStringAtPos(Ljava/lang/String;Z)V", shift = At.Shift.BEFORE)], cancellable = true)
    private fun `renderString$Inject$INVOKE$renderStringAtPos`(text: String, x: Float, y: Float, color: Int, shadow: Boolean, cir: CallbackInfoReturnable<Int>) {
        if (MetaMod.isReady()) {
            if (!CustomFont.overrideMinecraft && Emoji.isEnabled && text.contains(":")) {
                val textVar = Emoji.renderText(text, this.field_78288_b, shadow, this.field_78295_j, this.field_78296_k, this.field_78305_q)
                GlStateManager.color(this.field_78291_n, this.field_78292_o, this.field_78306_p, this.field_78305_q)
                this.func_78255_a(textVar, shadow)
                cir.returnValue = this.field_78295_j.toInt()
                return
            }
            if (text.contains("§*")) {
                this.`meta$renderWithCustomCodes`(text, shadow)
                cir.returnValue = this.field_78295_j.toInt()
            }
        }
    }

    @Inject(method = ["getStringWidth"], at = [At("HEAD")], cancellable = true)
    fun `getStringWidth$Inject$HEAD`(text: String, cir: CallbackInfoReturnable<Int>) {
        if (MetaMod.isReady() && CustomFont.overrideMinecraft) {
            if (Emoji.isEnabled && text.contains(":")) {
                cir.returnValue = Emoji.getStringWidthCustomFont(text)
            } else {
                cir.returnValue = MathUtilKt.ceilToInt(MainFontRenderer.getWidth(text).toFloat())
            }
        }
    }

    @Inject(method = ["getStringWidth"], at = [At("TAIL")], cancellable = true)
    fun `getStringWidth$Inject$TAIL`(text: String, cir: CallbackInfoReturnable<Int>) {
        if (MetaMod.isReady() && cir.returnValue != 0 && !CustomFont.overrideMinecraft && Emoji.isEnabled && text.contains(":")) {
            cir.returnValue = Emoji.getStringWidth(cir.returnValue, text, this.field_78288_b)
        }
    }

    @Inject(method = ["getCharWidth"], at = [At("HEAD")], cancellable = true)
    fun `getCharWidth$Inject$HEAD`(character: Char, cir: CallbackInfoReturnable<Int>) {
        if (MetaMod.isReady() && CustomFont.overrideMinecraft) {
            cir.returnValue = MathUtilKt.ceilToInt(MainFontRenderer.getWidth(character).toFloat())
        }
    }

    @Unique
    private fun processNamedColors(text: String?): String {
        if (text == null || !text.contains("<**$(")) {
            return text ?: ""
        }
        val matcher = COLOR_PATTERN.matcher(text)
        val result = StringBuilder()
        var lastEnd = 0
        while (matcher.find()) {
            result.append(text.substring(lastEnd, matcher.start()))
            val colorName = matcher.group(1).lowercase()
            val length = matcher.group(2).toInt()
            val hexColor = COLOR_SHORTCUTS[colorName]
            if (hexColor != null) {
                val textStart = matcher.end()
                val textEnd = Math.min(textStart + length, text.length)
                if (textStart < text.length) {
                    val coloredText = text.substring(textStart, textEnd)
                    result.append("§#").append(hexColor).append(coloredText).append("§r")
                    lastEnd = textEnd
                    continue
                }
                lastEnd = matcher.end()
                continue
            }
            result.append(matcher.group(0))
            lastEnd = matcher.end()
        }
        if (lastEnd < text.length) {
            result.append(text.substring(lastEnd))
        }
        return result.toString()
    }

    @Unique
    private fun processHexColors(text: String?): String {
        if (text == null || !text.contains("<**#(")) {
            return text ?: ""
        }
        val matcher = HEX_COLOR_PATTERN.matcher(text)
        val result = StringBuilder()
        var lastEnd = 0
        while (matcher.find()) {
            result.append(text.substring(lastEnd, matcher.start()))
            val hexColor = matcher.group(1).uppercase()
            val length = matcher.group(2).toInt()
            val textStart = matcher.end()
            val textEnd = Math.min(textStart + length, text.length)
            if (textStart < text.length) {
                val coloredText = text.substring(textStart, textEnd)
                result.append("§#").append(hexColor).append(coloredText).append("§r")
                lastEnd = textEnd
                continue
            }
            lastEnd = matcher.end()
        }
        if (lastEnd < text.length) {
            result.append(text.substring(lastEnd))
        }
        return result.toString()
    }

    @Unique
    private fun `meta$removeAllColorCodes`(text: String?): String? {
        var textVar = text ?: return null
        textVar = textVar.replace("<\\*\\*\\$\([a-zA-Z]+\)\\+\\d+->".toRegex(), "")
        textVar = textVar.replace("<\\*\\*#\\(\\([0-9A-Fa-f]{6}\\)\\+\\d+->".toRegex(), "")
        return textVar
    }

    @Unique
    private fun `meta$renderWithCustomCodes`(text: String, shadow: Boolean) {
        val originalColor = this.field_78304_r
        var italic = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '§' && i + 1 < text.length) {
                val code = text[++i].lowercaseChar()
                if (code == '*') {
                    val customColor = Settings.chatColor.rgb
                    this.field_78291_n = (customColor shr 16 and 0xFF).toFloat() / 255.0f
                    this.field_78306_p = (customColor shr 8 and 0xFF).toFloat() / 255.0f
                    this.field_78292_o = (customColor and 0xFF).toFloat() / 255.0f
                    this.field_78304_r = customColor
                    italic = false
                    this.func_78265_b()
                    continue
                }
                if (code == 'r') {
                    val origColor = originalColor
                    this.field_78291_n = (origColor shr 16 and 0xFF).toFloat() / 255.0f
                    this.field_78306_p = (origColor shr 8 and 0xFF).toFloat() / 255.0f
                    this.field_78292_o = (origColor and 0xFF).toFloat() / 255.0f
                    this.field_78304_r = originalColor
                    italic = false
                    this.func_78265_b()
                    continue
                }
                val fmt = this.`meta$getByFormattingCode`(code)
                if (fmt != null) {
                    if (fmt.isColor) {
                        // Handle colors
                    }
                    if (fmt == TextFormatting.ITALIC) {
                        italic = true
                    }
                }
            } else {
                this.func_181559_a(c, italic)
            }
            i++
        }
        val origColor = originalColor
        this.field_78291_n = (origColor shr 16 and 0xFF).toFloat() / 255.0f
        this.field_78306_p = (origColor shr 8 and 0xFF).toFloat() / 255.0f
        this.field_78292_o = (origColor and 0xFF).toFloat() / 255.0f
        this.field_78304_r = originalColor
    }

    @Unique
    private fun `meta$getByFormattingCode`(code: Char): TextFormatting? {
        val lowerCode = code.lowercaseChar()
        for (fmt in TextFormatting.values()) {
            val fmtStr = fmt.toString()
            if (fmtStr.length < 2 || fmtStr[1].lowercaseChar() != lowerCode) continue
            return fmt
        }
        return null
    }

    companion object {
        @Unique
        private val COLOR_PATTERN = Pattern.compile("<\\*\\*\\$\(([a-zA-Z]+)\)\\+(\\d+)->")
        @Unique
        private val HEX_COLOR_PATTERN = Pattern.compile("<\\*\\*#\\(\\([0-9A-Fa-f]{6}\\)\\+\\d+->")
        @Unique
        private val COLOR_SHORTCUTS = mutableMapOf<String, String>().apply {
            put("purple", "D8A1FF")
            put("lightpurple", "D8A1FF")
            put("pink", "FF69B4")
            put("hotpink", "FF1493")
            put("cyan", "00FFFF")
            put("aqua", "00FFFF")
            put("lime", "00FF00")
            put("gold", "FFD700")
            put("orange", "FFA500")
            put("red", "FF0000")
            put("darkred", "8B0000")
            put("blue", "0000FF")
            put("darkblue", "00008B")
            put("green", "008000")
            put("darkgreen", "006400")
            put("yellow", "FFFF00")
            put("white", "FFFFFF")
            put("black", "000000")
            put("gray", "808080")
            put("darkgray", "404040")
            put("lightgray", "C0C0C0")
        }
    }
}
