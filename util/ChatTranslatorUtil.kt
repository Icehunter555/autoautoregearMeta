package dev.wizard.meta.util

import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.pow

object ChatTranslatorUtil {
    private const val GOOGLE_TRANSLATE_URL = "https://translate.google.com/translate_a/single"

    fun getDisplayLanguage(languageCode: String): String {
        return Locale(languageCode).getDisplayLanguage(Locale.getDefault())
    }

    private fun generateURL(sourceLanguage: String, targetLanguage: String, text: String): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        return "https://translate.google.com/translate_a/single?client=webapp&hl=en&sl=$sourceLanguage&tl=$targetLanguage&q=$encoded&multires=1&otf=0&pc=0&trs=1&ssel=0&tsel=0&kc=1&dt=t&ie=UTF-8&oe=UTF-8&tk=${generateToken(text)}"
    }

    @Throws(IOException::class)
    fun detectLanguage(text: String): String? {
        val urlText = generateURL("auto", "en", text)
        val url = URL(urlText)
        val rawData = urlToText(url)
        return findLanguage(rawData)
    }

    @Throws(IOException::class)
    fun translate(text: String): String? {
        return translate(Locale.getDefault().language, text)
    }

    @Throws(IOException::class)
    fun translate(targetLanguage: String, text: String): String? {
        return translate("auto", targetLanguage, text)
    }

    @Throws(IOException::class)
    fun translate(sourceLanguage: String, targetLanguage: String, text: String): String? {
        val urlText = generateURL(sourceLanguage, targetLanguage, text)
        val url = URL(urlText)
        val rawData = urlToText(url)
        val raw = rawData.split("\"")
        if (raw.size < 2) {
            return null
        }
        return decodeUnicode(raw[1])
    }

    private fun urlToText(url: URL): String {
        val urlConn = url.openConnection()
        urlConn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:2.0) Gecko/20100101 Firefox/4.0")
        val buf = StringBuilder()
        InputStreamReader(urlConn.getInputStream(), StandardCharsets.UTF_8).use { r ->
            var ch: Int
            while (r.read().also { ch = it } != -1) {
                buf.append(ch.toChar())
            }
        }
        return buf.toString()
    }

    private fun findLanguage(rawData: String): String? {
        var i = 0
        while (i + 5 < rawData.length) {
            val dashDetected = rawData[i + 4] == '-'
            if (rawData[i] == ',' && rawData[i + 1] == '"' && (rawData[i + 4] == '"' && rawData[i + 5] == ',' || dashDetected)) {
                if (dashDetected) {
                    val string = rawData.substring(i + 2)
                    val lastQuote = string.indexOf('"')
                    if (lastQuote > 0) {
                        return rawData.substring(i + 2, i + 2 + lastQuote)
                    }
                } else {
                    val possible = rawData.substring(i + 2, i + 4)
                    if (containsLettersOnly(possible)) {
                        return possible
                    }
                }
            }
            i++
        }
        return null
    }

    private fun containsLettersOnly(text: String): Boolean {
        return text.all { Character.isLetter(it) }
    }

    private fun TKK(): IntArray {
        return intArrayOf(406398, 2087938574)
    }

    private fun shr32(x: Int, bits: Int): Int {
        return if (x < 0) {
            val xL = 0xFFFFFFFFL + x + 1L
            (xL shr bits).toInt()
        } else {
            x shr bits
        }
    }

    private fun RL(a: Int, b: String): Int {
        var result = a
        var c = 0
        while (c < b.length - 2) {
            var d = b[c + 2].code
            d = if (d >= 65) d - 87 else d - 48
            d = if (b[c + 1] == '+') shr32(result, d) else result shl d
            result = if (b[c] == '+') result + d else result xor d
            c += 3
        }
        return result
    }

    private fun generateToken(text: String): String {
        val tkk = TKK()
        val b = tkk[0]
        var e = 0
        val d = mutableListOf<Int>()
        var f = 0
        while (f < text.length) {
            var g = text[f].code
            if (128 > g) {
                d.add(e++, g)
            } else {
                if (2048 > g) {
                    d.add(e++, g shr 6 or 0xC0)
                } else if (0xD800 == (g and 0xFC00) && f + 1 < text.length && 0xDC00 == (text[f + 1].code and 0xFC00)) {
                    g = 65536 + ((g and 0x3FF) shl 10) + (text[++f].code and 0x3FF)
                    d.add(e++, g shr 18 or 0xF0)
                    d.add(e++, g shr 12 and 0x3F or 0x80)
                    d.add(e++, g shr 6 and 0x3F or 0x80)
                } else {
                    d.add(e++, g shr 12 or 0xE0)
                    d.add(e++, g shr 6 and 0x3F or 0x80)
                }
                d.add(e++, g and 0x3F or 0x80)
            }
            f++
        }
        var aI = b
        for (i in 0 until d.size) {
            aI += d[i]
            aI = RL(aI, "+-a^+6")
        }
        aI = RL(aI, "+-3^+b+-f")
        aI = aI xor tkk[1]
        val aL = if (0 > aI) 0x80000000L + (aI and Int.MAX_VALUE).toLong() else aI.toLong()
        val result = aL % 10.0.pow(6.0).toLong()
        return String.format(Locale.US, "%d.%d", result, result xor b.toLong())
    }

    private fun decodeUnicode(input: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length && input[i + 1] == 'u' && i + 5 < input.length) {
                try {
                    val hex = input.substring(i + 2, i + 6)
                    val code = hex.toInt(16)
                    result.append(code.toChar())
                    i += 6
                    continue
                } catch (e: NumberFormatException) {
                }
            }
            result.append(c)
            i++
        }
        return result.toString()
    }

    @Throws(IOException::class)
    fun translateIncomingOrOutgoing(text: String, target: String): String? {
        return translate("auto", target, text)
    }
}
