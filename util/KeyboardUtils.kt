package dev.wizard.meta.util

import dev.wizard.meta.util.extension.capitalize
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import java.util.*
import org.lwjgl.input.Keyboard

object KeyboardUtils {
    val allKeys: IntArray = IntArray(256) { it }

    private val displayNames: Array<String?> = Array(256) { i ->
        Keyboard.getKeyName(i)?.lowercase(Locale.ROOT)?.capitalize()
    }

    private val keyMap: Map<String, Int> = HashMap<String, Int>().apply {
        for (i in 0..255) {
            Keyboard.getKeyName(i)?.lowercase(Locale.ROOT)?.let { put(it, i) }
        }
        displayNames.forEachIndexed { index, name ->
            name?.lowercase(Locale.ROOT)?.let { put(it, index) }
        }
        put("ctrl", 29)
        put("alt", 56)
        put("shift", 42)
        put("meta", 219)
    }

    fun sendUnknownKeyError(bind: String) {
        NoSpamMessage.sendError(this, "Unknown key [${bind.formatValue()}]!", false)
    }

    fun getKey(keyName: String): Int {
        return keyMap[keyName.lowercase(Locale.ROOT)] ?: 0
    }

    @JvmStatic
    fun getKeyJava(keyName: String): Int {
        return keyMap[keyName.lowercase(Locale.ROOT)] ?: 0
    }

    fun getKeyName(keycode: Int): String? {
        return Keyboard.getKeyName(keycode)
    }

    fun getDisplayName(keycode: Int): String? {
        return displayNames.getOrNull(keycode)
    }
}
