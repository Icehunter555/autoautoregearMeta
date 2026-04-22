package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.manager.managers.MessageManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.ChatTranslatorUtil
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.MessageDetection
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import java.io.IOException

object ChatTranslator : Module(
    name = "ChatTranslator",
    category = Category.MISC,
    description = "Translates incoming and outgoing chat messages",
    modulePriority = 550,
    visible = true
) {
    private val translateOutgoing by setting("Translate Outgoing", true)
    private val outgoingLanguage by setting("Outgoing Language", Language.CHINESE_TRADITIONAL)
    private val translateIncoming by setting("Translate Incoming", true)
    private val incomingLanguage by setting("Incoming Language", Language.CHINESE_TRADITIONAL)
    private val ignoreCommands by setting("Ignore Commands", true)
    private val ignoreWhispers by setting("Ignore Whispers", false)
    private val onlyTranslateNonNative by setting("Auto Detect", true)
    private val showOriginal by setting("Show Original", true)
    private val prefixStyle by setting("Prefix Style", PrefixStyle.FANCY)

    private var isTranslating = false

    private val outgoingModifier = MessageManager.newMessageModifier(this, {
        if (!translateOutgoing) return@newMessageModifier false
        if (isTranslating) return@newMessageModifier false
        val message = it.packet.message
        if (MessageDetection.Command.ANY.detectNot(message)) return@newMessageModifier true
        if (ignoreCommands) return@newMessageModifier false
        true
    }, {
        val message = it.packet.message
        if (ignoreCommands && message.startsWith("/")) return@newMessageModifier message

        try {
            isTranslating = true
            val targetLang = outgoingLanguage.code
            val translated = ChatTranslatorUtil.translate("auto", targetLang, message)
            if (translated != null && translated != message) translated else message
        } catch (e: IOException) {
            NoSpamMessage.sendError("Translation failed: ${e.message}")
            message
        } finally {
            isTranslating = false
        }
    })

    init {
        onEnable {
            outgoingModifier.enable()
        }

        onDisable {
            outgoingModifier.disable()
        }

        listener<PacketEvent.Receive>(900) {
            if (!translateIncoming || it.isCancelled) return@listener
            if (it.packet is SPacketChat) {
                val message = it.packet.chatComponent.unformattedText
                if (message.isEmpty()) return@listener

                val messageInfo = parseMessage(message)

                if (ignoreWhispers && messageInfo.isWhisper) return@listener
                if (ignoreCommands && messageInfo.content.startsWith("/")) return@listener
                if (message.contains("[Translated]") || message.contains("[Translation]")) return@listener
                if (messageInfo.content.length < 2) return@listener

                try {
                    val targetLang = incomingLanguage.code
                    val shouldTranslate = if (onlyTranslateNonNative) {
                        val detectedLang = ChatTranslatorUtil.detectLanguage(messageInfo.content)
                        detectedLang != null && detectedLang != targetLang && !detectedLang.startsWith(targetLang)
                    } else {
                        true
                    }

                    if (!shouldTranslate) return@listener

                    val translated = ChatTranslatorUtil.translate("auto", targetLang, messageInfo.content)

                    if (translated != null && translated != messageInfo.content) {
                        val prefix = when (prefixStyle) {
                            PrefixStyle.FANCY -> "${TextFormatting.LIGHT_PURPLE}[${TextFormatting.GOLD}Translated${TextFormatting.LIGHT_PURPLE}]${TextFormatting.RESET}"
                            PrefixStyle.SIMPLE -> "${TextFormatting.GRAY}[Translated]${TextFormatting.RESET}"
                            PrefixStyle.NONE -> ""
                        }

                        val senderInfo = if (messageInfo.sender != null) {
                            if (messageInfo.isWhisper) "${TextFormatting.AQUA}${messageInfo.sender} whispers: ${TextFormatting.RESET}"
                            else "${TextFormatting.WHITE}<${messageInfo.sender}>${TextFormatting.RESET} "
                        } else ""

                        val translatedMessage = if (prefixStyle != PrefixStyle.NONE) "$prefix $senderInfo$translated" else "$senderInfo$translated"
                        
                        NoSpamMessage.sendRaw(translatedMessage, false)

                        if (showOriginal) {
                            val originalPrefix = "${TextFormatting.DARK_GRAY}[Original]${TextFormatting.RESET}"
                            NoSpamMessage.sendRaw("$originalPrefix $senderInfo${messageInfo.content}", false)
                        }
                    }
                } catch (e: IOException) {
                    // Ignore translation errors
                }
            }
        }
    }

    private fun parseMessage(message: String): MessageInfo {
        if (message.contains(" whispers: ")) {
            val sender = message.substringBefore(" whispers:").trim()
            val content = message.substringAfter(" whispers: ").trim()
            return MessageInfo(content, sender, true)
        }
        if (message.startsWith("<") && message.contains(">")) {
            val sender = message.substringAfter("<").substringBefore(">").trim()
            val content = message.substringAfter(">").trim()
            return MessageInfo(content, sender, false)
        }
        return MessageInfo(message, null, false)
    }

    fun shouldSkipEncryptedMessage(message: String): Boolean {
        return ChatEncrypt.isEnabled && ChatEncrypt.isEncryptedMessage(message)
    }

    private data class MessageInfo(val content: String, val sender: String?, val isWhisper: Boolean)

    private enum class PrefixStyle(override val displayName: CharSequence) : DisplayEnum {
        FANCY("Fancy"),
        SIMPLE("Simple"),
        NONE("None")
    }

    private enum class Language(override val displayName: CharSequence, val code: String) : DisplayEnum {
        AUTO("Auto Detect", "auto"),
        ENGLISH("English", "en"),
        SPANISH("Spanish", "es"),
        FRENCH("French", "fr"),
        GERMAN("German", "de"),
        ITALIAN("Italian", "it"),
        PORTUGUESE("Portuguese", "pt"),
        RUSSIAN("Russian", "ru"),
        JAPANESE("Japanese", "ja"),
        KOREAN("Korean", "ko"),
        CHINESE_SIMPLIFIED("Chinese (Simplified)", "zh-CN"),
        CHINESE_TRADITIONAL("Chinese (Traditional)", "zh-TW"),
        ARABIC("Arabic", "ar"),
        HINDI("Hindi", "hi"),
        DUTCH("Dutch", "nl"),
        POLISH("Polish", "pl"),
        TURKISH("Turkish", "tr"),
        SWEDISH("Swedish", "sv"),
        DANISH("Danish", "da"),
        NORWEGIAN("Norwegian", "no"),
        FINNISH("Finnish", "fi"),
        GREEK("Greek", "el"),
        CZECH("Czech", "cs"),
        ROMANIAN("Romanian", "ro"),
        HUNGARIAN("Hungarian", "hu"),
        VIETNAMESE("Vietnamese", "vi"),
        THAI("Thai", "th"),
        INDONESIAN("Indonesian", "id"),
        UKRAINIAN("Ukrainian", "uk"),
        HEBREW("Hebrew", "he")
    }
}
