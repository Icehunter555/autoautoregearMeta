package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.manager.managers.MessageManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.ChatTextUtils
import dev.wizard.meta.util.text.MessageDetection
import kotlin.math.min

object MessageModifier : Module(
    name = "MessageModifier",
    alias = arrayOf("suffix", "fancychat", "chatsuffix"),
    category = Category.MISC,
    description = "Modify your chat messages",
    modulePriority = 500
) {
    val antiSpamBypass by setting("Bypass anti spam", false)
    private val antiSpamBypassAmount by setting("Anti spam bypass amount", 3, 1..15, 1) { antiSpamBypass }
    private val commands by setting("Commands", false)
    private val suffix by setting("Suffix", Suffix.NONE)
    private val octoHackPlus by setting("OctoHack Plus", false) { suffix == Suffix.OCTOHACK }
    private val perryPhobos by setting("Perry Phobos", false) { suffix == Suffix.PHOBOS }
    private val snowFlake by setting("SnowFlake", false) { suffix == Suffix.SNOW }
    private val customSuffix by setting("Custom Suffix", "Meta") { suffix == Suffix.CUSTOM }
    private val customSuffixMode by setting("Custom Suffix Mode", CustomSuffixMode.NORMAL) { suffix == Suffix.CUSTOM }
    private val metaSuffix by setting("Meta Suffix", MetaSuffix.FUTURE) { suffix == Suffix.META }
    private val separator by setting("Separator", Separator.NONE) { suffix != Suffix.NONE }
    private val customSeparator by setting("Custom Separator", "|") { suffix != Suffix.NONE && separator == Separator.CUSTOM }
    private val double by setting("Double Suffix", false) { suffix != Suffix.NONE }
    private val prefix by setting("Prefix", Prefix.NONE)
    private val customPrefix by setting("Custom Prefix", ">") { prefix == Prefix.CUSTOM }
    private val fancyChat by setting("Fancy Chat", FancyChatMode.NONE)
    private val randomCase by setting("Random Case", true) { fancyChat == FancyChatMode.MOCK }
    private val notSpammer by setting("Not Spammer", true) { fancyChat != FancyChatMode.NONE }

    private val suffixModifier = MessageManager.newMessageModifier(this, {
        if (!commands && !MessageDetection.Command.ANY.detectNot(it.packet.message)) return@newMessageModifier false
        if (ChatEncrypt.isEncryptedMessage(it.packet.message)) return@newMessageModifier false
        true
    }, {
        var message = it.packet.message
        message = message.substring(0, min(256, message.length))

        if (prefix != Prefix.NONE) {
            message = getPrefix() + it.packet.message
        }

        if (antiSpamBypass) {
            message += " [${ChatTextUtils.generateRandomSuffix(antiSpamBypassAmount)}]"
        }

        if (suffix != Suffix.NONE) {
            val suffixText = if (suffix == Suffix.META) getMetaSuffix() else getChatSuffix()
            val sep = getSeparator()
            message += if (double) "$sep $suffixText $sep $suffixText" else "$sep $suffixText"
        }
        message
    })

    private val fancyChatModifier = MessageManager.newMessageModifier(this, {
        if (!commands && !MessageDetection.Command.ANY.detectNot(it.packet.message)) return@newMessageModifier false
        if (notSpammer && it.source is AutoSpam) return@newMessageModifier false
        if (fancyChat == FancyChatMode.NONE) return@newMessageModifier false
        true
    }, {
        var message = it.packet.message
        message = message.substring(0, min(256, message.length))

        when (fancyChat) {
            FancyChatMode.LEET -> ChatTextUtils.leetConverter(message)
            FancyChatMode.UNICODE -> ChatTextUtils.unicodeConverter(message)
            FancyChatMode.SMALLCAPS -> ChatTextUtils.smallCapsConverter(message)
            FancyChatMode.MOCK -> ChatTextUtils.mockConverter(message, randomCase)
            else -> message
        }
    })

    init {
        onEnable {
            suffixModifier.enable()
            fancyChatModifier.enable()
        }

        onDisable {
            suffixModifier.disable()
            fancyChatModifier.disable()
        }
    }

    private fun getChatSuffix(): String {
        return when (suffix) {
            Suffix.TROLLHACK -> "\uff34\uff32\uff2f\uff2c\uff2c\uff28\uff21\uff23\uff2b"
            Suffix.SKID -> "\u1d1b\u0280\u1d0f\u029f\u029f\ua731\u1d0b\u026a\u1d05"
            Suffix.EARTHHACK -> "\u00b3\u1d00\u0280\u1d1b\u029c\u029c\u2074\u1d04\u1d0b"
            Suffix.RUSHERHACK -> "\u02b3\u1d58\u02e2\u02b0\u1d49\u02b3\u02b0\u1d43\u1d9c\u1d4f"
            Suffix.KONAS -> "\uff2b\uff4f\uff4e\uff41\uff53"
            Suffix.PHOBOS -> if (perryPhobos) "\u1d29\u1d07\u0280\u0280\u1203\ua731 \u1d29\u029c\u1d0f\u0299\u1d0f\ua731" else "\u1d18\u029c\u1d0f\u0299\u1d0f\ua731"
            Suffix.CATALYST -> "\ua731\u1d07\u1d18\u1d18\u1d1c\u1d0b\u1d1c"
            Suffix.KAMIBLUE -> "\u1d0b\u1d00\u1d0d\u026a \u0299\u029f\u1d1c\u1d07"
            Suffix.KAMI -> "\u1d0b\u1d00\u1d0d\u026a"
            Suffix.GAMESENSE -> "\u0262\u1d00\u1d0d\u1d07\u0455\u1d07\u0274\u0455\u1d07"
            Suffix.GSPLUSPLUS -> "\u1d33\u02e2\u207a\u207a"
            Suffix.SEPPUKU -> "Seppuku" // Using enum name as fallback if string missing in when block
            Suffix.METEOR -> "Meteor On Crack"
            Suffix.BOZE -> "\uff22\uff4f\uff5a\uff45"
            Suffix.SNOW -> if (snowFlake) "\u2744" else "\ua731\u0274\uff10\u1d21"
            Suffix.SEXMASTER -> "\uff33\uff45\ua1d3\uff2d\u039b\uff53\u01ac\u03b5\u0280\uff0e\uff23\uff23"
            Suffix.OCTOHACK -> if (octoHackPlus) "\u2733\u039e\uff2f\u1d04\u1d1b\u0e4f\u0266\u039b\u1d04\u13e6 \u20b1\u2c60\u1458\u0586\u039e\u2733" else "\u039e\uff2f\u1d04\u1d1b\u0e4f\u0266\u039b\u1d04\u13e6\u039e"
            Suffix.PYRO -> "  \u0489 \u1d18\u028f\u0280\u1d0f \u1d04\u029f\u026a\u1d07\u0274\u1d1b \u0489"
            Suffix.DOTGOD -> "\uff24\uff4f\uff54\uff27\uff4f\uff44\uff0e\uff23\uff23"
            Suffix.LEMON -> "\u2113\u0454\u043c\u2134\u0e20"
            Suffix.CATMI -> "\u1d04\u1d00\u1d1b\u1d0d\u026a"
            Suffix.CUSTOM -> gimmeCustomSuffix()
            else -> ""
        }
    }

    private fun getMetaSuffix(): String {
        return when (metaSuffix) {
            MetaSuffix.FUTURE -> "\u722a\u4e47\u3112\u5342"
            MetaSuffix.JAP -> "(\u3063\u25d4\u25e1\u25d4)\u3063 \u2665 META \u2665"
            MetaSuffix.CUTE -> "\uff2d\uff25\uff34\uff21\u3000\uff08\u30b9\u30de\u7dda\uff09"
            MetaSuffix.SMALL -> "\u1d0d\u1d07\u1d1b\u1d00"
            MetaSuffix.UNICODE -> "\uff2d\uff25\uff34\uff21"
            MetaSuffix.SUPERSCRIPT -> "\u1d39\u1d31\u1d40\u1d2c"
            MetaSuffix.BACKWARDS -> "\u0250\u0287\u01dd\u026f"
            MetaSuffix.BOXED -> "\u24dc\u24d4\u24e3\u24d0"
            MetaSuffix.CURLY -> "\u0e53\u0454\u0547\u0e04"
            MetaSuffix.CENTERED -> "\uff4d\u15f4\u24e3\u1d43"
            MetaSuffix.GREEK -> "\uff4d\u039e\u01ac\u039b"
            MetaSuffix.FANCY -> "\ua0b5\ua3c2\ua4c5\ua2ec"
            MetaSuffix.JOINED -> "\u039c\u0395\u03a4\u0391"
        }
    }

    private fun getSeparator(): String {
        return when (separator) {
            Separator.BAR -> " | "
            Separator.ARROW -> " \u239f "
            Separator.TALLARROW -> " \u2794 "
            Separator.DOUBLESLASH -> " \u25ba "
            Separator.DOUBLEBAR -> " \u2016 "
            Separator.MIDDLE -> " \u2afd "
            Separator.ARROWHEAD -> " \u27e9\u27e9 "
            Separator.TRIPLESLASH -> " \u2022 "
            Separator.TRIANGLE -> " \u2afb "
            Separator.SMALLARROW -> " \u2023 "
            Separator.DOUBLEARROWHEAD -> " \u2192 "
            Separator.SQUARE -> " \u226b "
            Separator.STACKEDARROWS -> " \u25aa "
            Separator.CUSTOM -> " $customSeparator "
            else -> " "
        }
    }

    private fun gimmeCustomSuffix(): String {
        return when (customSuffixMode) {
            CustomSuffixMode.NORMAL -> customSuffix
            CustomSuffixMode.UNICODE -> ChatTextUtils.unicodeConverter(customSuffix)
            CustomSuffixMode.SMALLCAPS -> ChatTextUtils.smallCapsConverter(customSuffix)
        }
    }

    private fun getPrefix(): String {
        return when (prefix) {
            Prefix.GREEN -> "> "
            Prefix.SUFFIX -> (if (suffix != Suffix.META) getChatSuffix() else getMetaSuffix()) + " " + getSeparator() + " "
            Prefix.SEPARATOR -> getSeparator() + " "
            Prefix.CUSTOM -> customPrefix + " "
            Prefix.NONE -> ""
        }
    }

    private enum class CustomSuffixMode(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"),
        UNICODE("Unicode"),
        SMALLCAPS("Small Caps")
    }

    private enum class FancyChatMode(override val displayName: CharSequence) : DisplayEnum {
        NONE("None"),
        LEET("L33T"),
        UNICODE("Unicode"),
        SMALLCAPS("Small Caps"),
        MOCK("MoCk")
    }

    private enum class MetaSuffix(override val displayName: CharSequence) : DisplayEnum {
        FUTURE("Futuristic"),
        JAP("Jap"),
        CUTE("Cute"),
        SMALL("Small Caps"),
        UNICODE("Unicode"),
        SUPERSCRIPT("SuperScript"),
        BACKWARDS("Reversed"),
        BOXED("Boxxed"),
        CURLY("Curly"),
        CENTERED("Centered"),
        GREEK("Greek Caps"),
        FANCY("Fancy"),
        JOINED("Joined")
    }

    private enum class Prefix(override val displayName: CharSequence) : DisplayEnum {
        GREEN("Green"),
        SUFFIX("Suffix"),
        SEPARATOR("Separator"),
        CUSTOM("Custom"),
        NONE("None")
    }

    private enum class Separator(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"),
        BAR("Bar"),
        ARROW("Arrow"),
        TALLARROW("Tall Arrow"),
        DOUBLESLASH("Double Slashes"),
        DOUBLEBAR("Double Bars"),
        MIDDLE("Dot"),
        ARROWHEAD("Arrowhead"),
        TRIPLESLASH("Triple Slash"),
        TRIANGLE("Triangle"),
        SMALLARROW("Small Arrow"),
        DOUBLEARROWHEAD("Double ArrowHead"),
        SQUARE("Square"),
        STACKEDARROWS("Stacked Arrows"),
        CUSTOM("Custom"),
        NONE("None")
    }

    private enum class Suffix(override val displayName: CharSequence) : DisplayEnum {
        META("Meta Suffixes"),
        TROLLHACK("TrollHack"),
        SKID("TrollSkid"),
        EARTHHACK("Earthhack"),
        RUSHERHACK("Rusher"),
        KONAS("Konas"),
        PHOBOS("Phobos"),
        CATALYST("Catalyst"),
        KAMIBLUE("Kami Blue"),
        KAMI("Kami"),
        GAMESENSE("GameSense"),
        GSPLUSPLUS("GameSense Plus Plus"),
        SEPPUKU("Seppuku"),
        METEOR("Meteor"),
        BOZE("Boze"),
        SNOW("Sn0w"),
        SEXMASTER("SexMaster"),
        OCTOHACK("OctoHack"),
        PYRO("Pyro"),
        DOTGOD("DotGod"),
        LEMON("Lemon"),
        CATMI("Catmi"),
        CUSTOM("Custom"),
        NONE("None")
    }
}