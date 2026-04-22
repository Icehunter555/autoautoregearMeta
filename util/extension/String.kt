package dev.wizard.meta.util.extension

import java.util.*

fun String.max(max: Int): String {
    return substring(0, length.coerceAtMost(max))
}

fun String.max(max: Int, suffix: String): String {
    return if (length > max) max(max - suffix.length) + suffix else max(max)
}

fun String.surroundedBy(prefix: CharSequence, suffix: CharSequence, ignoreCase: Boolean = false): Boolean {
    return startsWith(prefix, ignoreCase) && endsWith(suffix, ignoreCase)
}

fun String.surroundedBy(prefix: Char, suffix: Char, ignoreCase: Boolean = false): Boolean {
    return startsWith(prefix, ignoreCase) && endsWith(suffix, ignoreCase)
}

fun String.mapEach(delimiters: CharArray, transformer: (String) -> String): List<String> {
    return split(*delimiters.toTypedArray()).map(transformer)
}

fun String.capitalize(): String {
    if (isEmpty()) return this
    val it = this[0]
    val c = if (it.isLowerCase()) Character.toTitleCase(it) else it
    return c.toString() + substring(1)
}

fun String.normalizeCase(): String {
    val charArray = CharArray(length)
    for ((i, c) in withIndex()) {
        charArray[i] = if (i == 0) Character.toTitleCase(c) else Character.toLowerCase(c)
    }
    return String(charArray)
}

fun String.mapIndexed(transformer: (IndexedValue<Char>) -> Char): String {
    val charArray = CharArray(length)
    for (it in withIndex()) {
        charArray[it.index] = transformer(it)
    }
    return String(charArray)
}

fun String.map(transformer: (Char) -> Char): String {
    val charArray = CharArray(length)
    for (i in indices) {
        charArray[i] = transformer(this[i])
    }
    return String(charArray)
}

fun String.remove(c: Char): String {
    return buildString {
        for (char in this@remove) {
            if (char != c) append(char)
        }
    }
}

fun String.remove(vararg chars: Char): String {
    return buildString {
        for (char in this@remove) {
            if (char !in chars) append(char)
        }
    }
}

fun String.remove(charSequence: CharSequence): String {
    val l = charSequence.length
    if (l == 0) return this
    return buildString {
        var i = 0
        while (i < this@remove.length) {
            if (i + l <= this@remove.length && this@remove.subSequence(i, i + l) == charSequence) {
                i += l
                continue
            }
            append(this@remove[i])
            i++
        }
    }
}

fun String.remove(vararg charSequences: CharSequence): String {
    return buildString {
        var i = 0
        outer@ while (i < this@remove.length) {
            for (cs in charSequences) {
                val l = cs.length
                if (l > 0 && i + l <= this@remove.length && this@remove.subSequence(i, i + l) == cs) {
                    i += l
                    continue@outer
                }
            }
            append(this@remove[i])
            i++
        }
    }
}
