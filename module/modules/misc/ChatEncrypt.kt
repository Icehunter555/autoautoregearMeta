package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.manager.managers.MessageManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.MessageDetection
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.onMainThread
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting
import java.util.Base64
import kotlin.math.min

object ChatEncrypt : Module(
    name = "ChatEncrypt",
    category = Category.MISC,
    description = "Encrypts outgoing messages and decrypts incoming encrypted messages",
    modulePriority = 600
) {
    private val sendMode by setting("Send Mode", SendMode.ALL)
    private val customPrefix by setting("Custom Prefix", "!") { sendMode == SendMode.PREFIX }
    private val encryptionMode by setting("Encryption Mode", EncryptionMode.CAESAR)
    private val caesarOffset by setting("Caesar Offset", 13, 1..25, 1) { encryptionMode == EncryptionMode.CAESAR }
    private val encryptionKey by setting("Encryption Key", "TrollHack") { encryptionMode == EncryptionMode.KEYBASED }
    private val commands by setting("Encrypt Commands", false)
    private val showDecrypted by setting("Show Decrypted", true)
    private val autoDetect by setting("Auto Detect", true)
    private val detectionMode by setting("Detection", Detection.CHATANDWHISPERS)

    private const val CAESAR_ID = "<c$>"
    private const val KEYBASED_ID = "<k%>"
    private const val BASE64_ID = "<b#>"

    private val encryptionModifier = MessageManager.newMessageModifier(this, {
        if (commands) return@newMessageModifier true
        val message = it.packet.message
        !MessageDetection.Command.ANY.detectNot(message)
    }, {
        var message = it.packet.message
        message = message.substring(0, min(200, message.length))

        if (sendMode == SendMode.PREFIX) {
            if (!message.startsWith(customPrefix)) return@newMessageModifier message
        }

        when (encryptionMode) {
            EncryptionMode.CAESAR -> CAESAR_ID + caesarEncrypt(message, caesarOffset)
            EncryptionMode.KEYBASED -> KEYBASED_ID + keyBasedEncrypt(message, encryptionKey)
            EncryptionMode.BASE64 -> BASE64_ID + base64Encrypt(message)
        }
    })

    init {
        onEnable {
            encryptionModifier.enable()
        }

        onDisable {
            encryptionModifier.disable()
        }

        listener<PacketEvent.Receive>(1000) {
            if (it.packet is SPacketChat) {
                val message = it.packet.chatComponent.unformattedText
                val detectionResult = detectEncryptedMessage(message)

                if (detectionResult.isEncrypted && showDecrypted) {
                    val decrypted = tryDecrypt(detectionResult.content)
                    if (decrypted != null) {
                        val prefix = if (detectionResult.isWhisper) "${TextFormatting.WHITE}[Decrypted Whisper from ${detectionResult.sender}]" else "${TextFormatting.WHITE}[Decrypted from ${detectionResult.sender}]"
                        NoSpamMessage.sendRaw("$prefix ${TextFormatting.GRAY}$decrypted${TextFormatting.RESET}", false)
                    }
                }
            }
        }
    }

    private fun detectEncryptedMessage(message: String): EncryptedDetectionResult {
        val isWhisper = message.contains(" whispers: ")
        if (isWhisper && detectionMode == Detection.CHATONLY) {
            return EncryptedDetectionResult(false, "", null, false)
        }

        val sender: String?
        val content: String

        if (isWhisper) {
            sender = message.substringBefore(" whispers:").trim()
            content = message.substringAfter(" whispers: ").trim()
        } else if (message.startsWith("<") && message.contains(">")) {
            sender = message.substringAfter("<").substringBefore(">").trim()
            content = message.substringAfter(">").trim()
        } else {
            return EncryptedDetectionResult(false, "", null, false)
        }

        val isEncrypted = content.startsWith(CAESAR_ID) || content.startsWith(KEYBASED_ID) || content.startsWith(BASE64_ID)
        return EncryptedDetectionResult(isEncrypted, content, sender, isWhisper)
    }

    private fun caesarEncrypt(text: String, offset: Int): String {
        return text.map { char ->
            when {
                char.isUpperCase() -> ((char - 'A' + offset) % 26 + 'A'.toInt()).toChar()
                char.isLowerCase() -> ((char - 'a' + offset) % 26 + 'a'.toInt()).toChar()
                else -> char
            }
        }.joinToString("")
    }

    private fun caesarDecrypt(text: String, offset: Int): String {
        return text.map { char ->
            when {
                char.isUpperCase() -> ((char - 'A' - offset + 26) % 26 + 'A'.toInt()).toChar()
                char.isLowerCase() -> ((char - 'a' - offset + 26) % 26 + 'a'.toInt()).toChar()
                else -> char
            }
        }.joinToString("")
    }

    private fun keyBasedEncrypt(text: String, key: String): String {
        if (key.isEmpty()) return text
        val encrypted = text.mapIndexed { index, char ->
            (char.toInt() xor key[index % key.length].toInt()).toChar()
        }.joinToString("")
        
        return encrypted.map { String.format("%02x", it.toInt()) }.joinToString("")
    }

    private fun keyBasedDecrypt(hexText: String, key: String): String? {
        if (key.isEmpty() || hexText.length % 2 != 0) return null
        return try {
            val chars = hexText.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
            chars.mapIndexed { index, char ->
                (char.toInt() xor key[index % key.length].toInt()).toChar()
            }.joinToString("")
        } catch (e: Exception) {
            null
        }
    }

    private fun base64Encrypt(text: String): String {
        return Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
    }

    private fun base64Decrypt(encoded: String): String? {
        return try {
            String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryDecrypt(message: String): String? {
        if (!autoDetect) return null

        if (message.startsWith(CAESAR_ID)) {
            val encrypted = message.removePrefix(CAESAR_ID)
            val decrypted = caesarDecrypt(encrypted, caesarOffset)

            if (autoDetect) {
                val count = decrypted.count { it.isLetterOrDigit() || it.isWhitespace() }
                if (count.toDouble() < decrypted.length * 0.8) {
                    for (offset in 1..25) {
                        val testDecrypted = caesarDecrypt(encrypted, offset)
                        val testCount = testDecrypted.count { it.isLetterOrDigit() || it.isWhitespace() }
                        if (testCount.toDouble() > testDecrypted.length * 0.8) {
                            return testDecrypted
                        }
                    }
                    return decrypted
                }
            }
            return decrypted
        }

        if (message.startsWith(KEYBASED_ID)) {
            val encrypted = message.removePrefix(KEYBASED_ID)
            return keyBasedDecrypt(encrypted, encryptionKey)
        }

        if (message.startsWith(BASE64_ID)) {
            val encoded = message.removePrefix(BASE64_ID)
            return base64Decrypt(encoded)
        }

        return null
    }

    fun isEncryptedMessage(message: String): Boolean {
        return message.startsWith(CAESAR_ID) || message.startsWith(KEYBASED_ID) || message.startsWith(BASE64_ID)
    }

    private data class EncryptedDetectionResult(val isEncrypted: Boolean, val content: String, val sender: String?, val isWhisper: Boolean)

    private enum class SendMode(override val displayName: CharSequence) : DisplayEnum {
        ALL("All"),
        PREFIX("Prefix")
    }

    private enum class EncryptionMode(override val displayName: CharSequence) : DisplayEnum {
        CAESAR("Caesar Cipher"),
        KEYBASED("Key-Based XOR"),
        BASE64("Base64 Obfuscation")
    }

    private enum class Detection(override val displayName: CharSequence) : DisplayEnum {
        CHATONLY("Chat Only"),
        CHATANDWHISPERS("Chat And Whispers")
    }
}
