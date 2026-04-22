package dev.wizard.meta.util.text

import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.interfaces.Helper
import dev.wizard.meta.util.threads.onMainThread
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import java.util.*

object NoSpamMessage : Helper {
    private val messageQueue = ArrayDeque<Int>()

    fun sendMessage(message: String, delete: Boolean = true) {
        send(messageFormatter(message = message), message.hashCode(), delete)
    }

    fun sendMessage(identifier: Any, message: String, delete: Boolean = true) {
        send(messageFormatter(message = message), identifier.hashCode(), delete)
    }

    fun sendWarning(message: String, delete: Boolean = true) {
        send(messageFormatter(MsgType.WARNING, message), message.hashCode(), delete)
    }

    fun sendWarning(identifier: Any, message: String, delete: Boolean = true) {
        send(messageFormatter(MsgType.WARNING, message), identifier.hashCode(), delete)
    }

    fun sendRaw(message: String, delete: Boolean = true) {
        onMainThread {
            val messageId = message.hashCode()
            if (delete) messageQueue.addLast(messageId)
            checkQueue()
            mc.ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(TextComponentString(message), messageId)
        }
    }

    fun sendRaw(identifier: Any, message: String, delete: Boolean = true) {
        onMainThread {
            val messageId = identifier.hashCode()
            if (delete) messageQueue.addLast(messageId)
            checkQueue()
            mc.ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(TextComponentString(message), messageId)
        }
    }

    fun sendError(message: String, delete: Boolean = false) {
        send(messageFormatter(MsgType.ERROR, message), message.hashCode(), delete)
    }

    fun sendError(identifier: Any, message: String, delete: Boolean = false) {
        send(messageFormatter(MsgType.ERROR, message), identifier.hashCode(), delete)
    }

    private fun send(message: String, id: Int, delete: Boolean) {
        onMainThread {
            if (delete) messageQueue.addLast(id)
            checkQueue()
            mc.ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(TextComponentString(message), id)
        }
    }

    private fun checkQueue() {
        if (messageQueue.size > 4) {
            val oldestId = messageQueue.removeFirst()
            mc.ingameGUI.chatGUI.deleteChatLine(oldestId)
        }
    }

    private fun messageFormatter(type: MsgType = MsgType.NORMAL, message: String): String {
        return "${getMessagePrefix(type)}  $message ${if (Settings.customPrefixColor && type == MsgType.NORMAL) TextFormatting.RESET else ""}"
    }

    private fun getMessagePrefix(type: MsgType): String {
        val prefixText = when (Settings.clientMessagePrefix) {
            Settings.ClientMessageText.NONE -> return ""
            Settings.ClientMessageText.LOWERCASEM -> "m"
            Settings.ClientMessageText.UPPERCASEM -> "M"
            Settings.ClientMessageText.LOWERCASEMETA -> "meta"
            Settings.ClientMessageText.UPPERCASEMETA -> "BETA"
            Settings.ClientMessageText.CAPITALIZEDMETA -> "Meta"
            Settings.ClientMessageText.CIRCLEM -> "Ⓜ"
            Settings.ClientMessageText.ARROW -> "->"
            Settings.ClientMessageText.COMMAND -> ">_"
            Settings.ClientMessageText.HASHTAG -> "#"
        }

        val prefixColor = when (type) {
            MsgType.NORMAL -> Settings.clientMessagePrefixColor
            MsgType.WARNING -> Settings.clientMessagePrefixWarningColor
            MsgType.ERROR -> Settings.clientMessagePrefixErrorColor
        }

        val formattedPrefix = "${TextFormatting.RESET}${if (Settings.customPrefixColor && type == MsgType.NORMAL) "§(§)" else prefixColor}${if (Settings.clientMessagePrefixBold) TextFormatting.BOLD else ""}${if (Settings.clientMessagePrefixUnderline) TextFormatting.UNDERLINE else ""}$prefixText${TextFormatting.RESET}"
        val bracketFormatting = "${if (Settings.clientMessageBracketsBold) TextFormatting.BOLD else ""}${if (Settings.clientMessageBracketsUnderline) TextFormatting.UNDERLINE else ""}${Settings.clientMessageBracketColor}"

        val (leftBracket, rightBracket) = when (Settings.clientMessageBrackets) {
            Settings.ClientMessageBrackets.NONE -> "" to ""
            Settings.ClientMessageBrackets.NORMAL -> "(" to ")"
            Settings.ClientMessageBrackets.SQUARE -> "[" to "]"
            Settings.ClientMessageBrackets.CURLY -> "{" to "}"
            Settings.ClientMessageBrackets.ANGLED -> "<" to ">"
            Settings.ClientMessageBrackets.DOUBLEANGLED -> "«" to "»"
            Settings.ClientMessageBrackets.CORNER -> "『" to "』"
            Settings.ClientMessageBrackets.ROUNDED_BLOCK -> "【" to "】"
        }

        return if (Settings.clientMessageBrackets == Settings.ClientMessageBrackets.NONE) {
            "$formattedPrefix${if (Settings.customPrefixColor && type == MsgType.NORMAL) TextFormatting.WHITE else ""}"
        } else {
            "${TextFormatting.RESET}$bracketFormatting$leftBracket$formattedPrefix$bracketFormatting$rightBracket${TextFormatting.RESET}${if (Settings.customPrefixColor && type == MsgType.NORMAL) TextFormatting.WHITE else ""}"
        }
    }

    private enum class MsgType {
        NORMAL, ERROR, WARNING
    }
}
