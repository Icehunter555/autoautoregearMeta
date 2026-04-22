package dev.wizard.meta.util.text

object EncryptionUtils {
    fun encryptCaesar(input: String?, caesarShift: Int): String {
        if (input == null) return ""
        val chars = input.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            chars[i] = when {
                c in 'a'..'z' -> (97 + (c.code - 97 + caesarShift) % 26).toChar()
                c in 'A'..'Z' -> (65 + (c.code - 65 + caesarShift) % 26).toChar()
                else -> c
            }
        }
        return String(chars)
    }

    fun decryptCaesar(input: String?, caesarShift: Int): String {
        if (input == null) return ""
        val chars = input.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            chars[i] = when {
                c in 'a'..'z' -> (97 + (c.code - 97 - caesarShift + 26) % 26).toChar()
                c in 'A'..'Z' -> (65 + (c.code - 65 - caesarShift + 26) % 26).toChar()
                else -> c
            }
        }
        return String(chars)
    }

    @JvmStatic
    fun obfuscateCompact(message: String?): String {
        if (message.isNullOrEmpty()) return ""
        return buildString {
            for (i in message.indices) {
                val c = message[i]
                append((c.code.inv() xor i).toChar())
            }
        }
    }

    @JvmStatic
    fun deobfuscateCompact(obfuscated: String?): String {
        if (obfuscated.isNullOrEmpty()) return ""
        return buildString {
            for (i in obfuscated.indices) {
                val c = obfuscated[i]
                append((c.code xor i).inv().toChar())
            }
        }
    }
}
