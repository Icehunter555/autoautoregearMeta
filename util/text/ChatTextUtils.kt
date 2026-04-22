package dev.wizard.meta.util.text

import dev.wizard.meta.util.math.MathUtils
import net.minecraft.network.play.server.SPacketChat
import kotlin.random.Random

object ChatTextUtils {
    fun leetSwitch(c: Char): Char {
        val c2 = c.lowercaseChar()
        return when (c2) {
            'a' -> '4'
            'b' -> '8'
            'e' -> '3'
            'g' -> '9'
            'l' -> '1'
            'o' -> '0'
            's' -> '5'
            't' -> '7'
            else -> c
        }
    }

    fun unicodeSwitch(c: Char): Char {
        return when (c) {
            'a' -> 65345.toChar()
            'b' -> 65346.toChar()
            'c' -> 65347.toChar()
            'd' -> 65348.toChar()
            'e' -> 65349.toChar()
            'f' -> 65350.toChar()
            'g' -> 65351.toChar()
            'h' -> 65352.toChar()
            'i' -> 65353.toChar()
            'j' -> 65354.toChar()
            'k' -> 65355.toChar()
            'l' -> 65356.toChar()
            'm' -> 65357.toChar()
            'n' -> 65358.toChar()
            'o' -> 65359.toChar()
            'p' -> 65360.toChar()
            'q' -> 65361.toChar()
            'r' -> 65362.toChar()
            's' -> 65363.toChar()
            't' -> 65364.toChar()
            'u' -> 65365.toChar()
            'v' -> 65366.toChar()
            'w' -> 65367.toChar()
            'x' -> 65368.toChar()
            'y' -> 65369.toChar()
            'z' -> 65370.toChar()
            'A' -> 65313.toChar()
            'B' -> 65314.toChar()
            'C' -> 65315.toChar()
            'D' -> 65316.toChar()
            'E' -> 65317.toChar()
            'F' -> 65318.toChar()
            'G' -> 65319.toChar()
            'H' -> 65320.toChar()
            'I' -> 65321.toChar()
            'J' -> 65322.toChar()
            'K' -> 65323.toChar()
            'L' -> 65324.toChar()
            'M' -> 65325.toChar()
            'N' -> 65326.toChar()
            'O' -> 65327.toChar()
            'P' -> 65328.toChar()
            'Q' -> 65329.toChar()
            'R' -> 65330.toChar()
            'S' -> 65331.toChar()
            'T' -> 65332.toChar()
            'U' -> 65333.toChar()
            'V' -> 65334.toChar()
            'W' -> 65335.toChar()
            'X' -> 65336.toChar()
            'Y' -> 65337.toChar()
            'Z' -> 65338.toChar()
            '1' -> 65297.toChar()
            '2' -> 65298.toChar()
            '3' -> 65299.toChar()
            '4' -> 65300.toChar()
            '5' -> 65301.toChar()
            '6' -> 65302.toChar()
            '7' -> 65303.toChar()
            '8' -> 65304.toChar()
            '9' -> 65305.toChar()
            '0' -> 65296.toChar()
            '!' -> 65281.toChar()
            '@' -> 65312.toChar()
            '#' -> 65283.toChar()
            '$' -> 65284.toChar()
            '%' -> 65285.toChar()
            '&' -> 65286.toChar()
            ':' -> 65306.toChar()
            ';' -> 65307.toChar()
            '|' -> 65372.toChar()
            ' ' -> 12288.toChar()
            else -> c
        }
    }

    fun smallCapsSwitch(c: Char): Char {
        return when (c) {
            'a', 'A' -> 7424.toChar()
            'b', 'B' -> 665.toChar()
            'c', 'C' -> 7428.toChar()
            'd', 'D' -> 7429.toChar()
            'e', 'E' -> 7431.toChar()
            'f', 'F' -> 42800.toChar()
            'g', 'G' -> 610.toChar()
            'h', 'H' -> 668.toChar()
            'i', 'I' -> 618.toChar()
            'j', 'J' -> 7434.toChar()
            'k', 'K' -> 7435.toChar()
            'l', 'L' -> 671.toChar()
            'm', 'M' -> 7437.toChar()
            'n', 'N' -> 628.toChar()
            'o', 'O' -> 7439.toChar()
            'p', 'P' -> 7448.toChar()
            'q', 'Q' -> 491.toChar()
            'r', 'R' -> 640.toChar()
            's', 'S' -> 42801.toChar()
            't', 'T' -> 7451.toChar()
            'u', 'U' -> 7452.toChar()
            'v', 'V' -> 7456.toChar()
            'w', 'W' -> 7457.toChar()
            'x', 'X' -> 'x'
            'y', 'Y' -> 655.toChar()
            'z', 'Z' -> 7458.toChar()
            else -> c
        }
    }

    fun mockConverter(input: String, randomCase: Boolean): String {
        return buildString {
            for (i in input.indices) {
                val c = input[i]
                val upper = if (randomCase) Random.nextBoolean() else MathUtils.isNumberEven(i)
                append(if (upper) c.uppercaseChar() else c.lowercaseChar())
            }
        }
    }

    fun unicodeConverter(input: String): String {
        return buildString {
            for (c in input) {
                append(unicodeSwitch(c))
            }
        }
    }

    fun leetConverter(input: String): String {
        return buildString {
            for (c in input) {
                append(leetSwitch(c))
            }
        }
    }

    fun smallCapsConverter(input: String): String {
        return buildString {
            for (c in input) {
                append(smallCapsSwitch(c))
            }
        }
    }

    fun generateRandomSuffix(amount: Int): String {
        val random = Random(System.currentTimeMillis())
        val numbers = '0'..'9'
        val letters = ('a'..'z').toList() + ('A'..'Z').toList()
        val allChars = numbers.toList() + letters
        
        val suffix = StringBuilder()
        suffix.append(numbers.random(random))
        if (amount > 1) {
            suffix.append(letters.random(random))
        }
        for (i in 2 until amount) {
            suffix.append(allChars.random(random))
        }
        
        val chars = suffix.substring(2).toMutableList()
        chars.shuffle(random)
        suffix.replace(2, suffix.length, chars.joinToString(""))
        return suffix.toString()
    }

    fun messageContains(chatPacket: SPacketChat, trigger: String, ignoreCase: Boolean = true): Boolean {
        val message = chatPacket.chatComponent.unformattedText
        return message.contains(trigger, ignoreCase)
    }

    fun getMessageSender(chatPacket: SPacketChat, checkWhisper: Boolean = true): String? {
        val message = chatPacket.chatComponent.unformattedText
        return when {
            checkWhisper && message.contains(" Whispers: ") -> message.substringBefore(" Whispers: ")
            message.startsWith("<") && message.contains(">") -> message.substringAfter("<").substringBefore(">")
            else -> null
        }
    }
}
