package dev.wizard.meta.module.modules.player

import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.accessor.textComponent
import dev.wizard.meta.util.text.MessageDetection
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.SpamFilters
import dev.wizard.meta.util.text.getUnformatted
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextComponentString
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object ChatFilter : Module(
    "ChatFilter",
    category = Category.PLAYER,
    description = "Removes spam and advertising from the chat"
) {
    private val mode by setting(this, EnumSetting(settingName("Mode"), Mode.REPLACE))
    private val replaceMode by setting(this, EnumSetting(settingName("Replace Mode"), ReplaceMode.ASTERISKS, mode.atValue(Mode.REPLACE)))
    private val page by setting(this, EnumSetting(settingName("Page"), Page.TYPE))

    private val discordLinks by setting(this, BooleanSetting(settingName("Discord"), true, page.atValue(Page.TYPE)))
    private val slurs by setting(this, BooleanSetting(settingName("Slurs"), true, page.atValue(Page.TYPE)))
    private val swears by setting(this, BooleanSetting(settingName("Swears"), false, page.atValue(Page.TYPE)))
    private val automated by setting(this, BooleanSetting(settingName("Automated"), true, page.atValue(Page.TYPE)))
    private val ips by setting(this, BooleanSetting(settingName("Server Ips"), true, page.atValue(Page.TYPE)))
    private val specialCharEnding by setting(this, BooleanSetting(settingName("Special Ending"), true, page.atValue(Page.TYPE)))
    private val specialCharBegin by setting(this, BooleanSetting(settingName("Special Begin"), true, page.atValue(Page.TYPE)))
    private val greenText by setting(this, BooleanSetting(settingName("Green Text"), false, page.atValue(Page.TYPE)))
    private val fancyChat by setting(this, BooleanSetting(settingName("Fancy Chat"), false, page.atValue(Page.TYPE)))

    private val aggressiveFiltering by setting(this, BooleanSetting(settingName("Aggressive Filtering"), true, page.atValue(Page.SETTINGS)))
    private val duplicates by setting(this, BooleanSetting(settingName("Duplicates"), true, page.atValue(Page.SETTINGS)))
    private val duplicatesTimeout by setting(this, IntegerSetting(settingName("Duplicates Timeout"), 30, 1..600, 5, page.atValue(Page.SETTINGS) and duplicates.atTrue()))
    private val filterOwn by setting(this, BooleanSetting(settingName("Filter Own"), false, page.atValue(Page.SETTINGS)))
    private val filterDMs by setting(this, BooleanSetting(settingName("Filter DMs"), false, page.atValue(Page.SETTINGS)))
    private val filterServer by setting(this, BooleanSetting(settingName("Filter Server"), false, page.atValue(Page.SETTINGS)))
    private val showBlocked by setting(this, EnumSetting(settingName("Show Blocked"), ShowBlocked.LOG_FILE, page.atValue(Page.SETTINGS)))

    private val messageHistory = ConcurrentHashMap<String, Long>()
    private val settingArray by lazy {
        arrayOf(
            discordLinks to SpamFilters.discordInvite,
            slurs to SpamFilters.slurs,
            swears to SpamFilters.swears,
            automated to SpamFilters.announcer,
            automated to SpamFilters.spammer,
            automated to SpamFilters.insulter,
            automated to SpamFilters.greeter,
            automated to SpamFilters.ownsMeAndAll,
            automated to SpamFilters.thanksTo,
            ips to SpamFilters.ipAddress,
            specialCharBegin to SpamFilters.specialBeginning,
            specialCharEnding to SpamFilters.specialEnding,
            greenText to SpamFilters.greenText
        )
    }

    init {
        onDisable {
            messageHistory.clear()
        }

        safeListener<PacketEvent.Receive>(9999) { event ->
            val packet = event.packet
            if (packet !is SPacketChat) return@safeListener

            messageHistory.values.removeIf { System.currentTimeMillis() - it > 600000L }

            if (duplicates) {
                if (checkDupes(packet.chatComponent.getUnformatted())) {
                    event.cancel()
                }
            }

            val pattern = isSpam(packet.chatComponent.getUnformatted())
            if (pattern != null) {
                if (mode == Mode.HIDE) {
                    event.cancel()
                } else if (mode == Mode.REPLACE) {
                    val formatted = packet.chatComponent.formattedText
                    packet.textComponent = TextComponentString(sanitize(formatted, pattern, replaceMode.redaction))
                }
            }

            if (fancyChat) {
                val formatted = packet.chatComponent.formattedText
                val sanitized = sanitizeFancyChat(formatted)
                if (sanitized.trim().isEmpty()) {
                    packet.textComponent = TextComponentString("${getUsername(formatted)} [Fancychat]")
                }
            }
        }
    }

    private fun sanitize(toClean: String, matcher: String, replacement: String): String {
        return if (!aggressiveFiltering) {
            toClean.replace(Regex("\\b$matcher|$matcher\\b"), replacement)
        } else {
            toClean.replace(Regex(matcher), replacement)
        }
    }

    private fun isSpam(message: String): String? {
        if (!filterOwn && isOwn(message)) return null
        if (!filterDMs && MessageDetection.Direct.ANY.detect(message)) return null
        if (!filterServer && MessageDetection.Server.ANY.detect(message)) return null
        return detectSpam(removeUsername(message))
    }

    private fun detectSpam(message: String): String? {
        for ((setting, patterns) in settingArray) {
            if (setting.value) {
                val pattern = findPatterns(patterns, message)
                if (pattern != null) {
                    sendResult(setting.internalName, message)
                    return pattern
                }
            }
        }
        return null
    }

    private fun removeUsername(username: String): String {
        return username.replace(Regex("<[^>]*> "), "")
    }

    private fun getUsername(rawMessage: String): String {
        val matcher = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE).matcher(rawMessage)
        return if (matcher.find()) matcher.group() else rawMessage.substring(0, rawMessage.indexOf(">").coerceAtLeast(0))
    }

    private fun checkDupes(message: String): Boolean {
        var isDuplicate = false
        messageHistory[message]?.let {
            if ((System.currentTimeMillis() - it) / 1000 < duplicatesTimeout) {
                isDuplicate = true
            }
        }
        messageHistory[message] = System.currentTimeMillis()
        if (isDuplicate) sendResult("Duplicate", message)
        return isDuplicate
    }

    private fun isOwn(message: String): Boolean {
        val ownFilter = "^<${mc.player?.name}> "
        return Pattern.compile(ownFilter, Pattern.CASE_INSENSITIVE).matcher(message).find()
    }

    private fun findPatterns(patterns: Array<String>, string: String): String? {
        val cString = string.replace(Regex("<[^>]*> "), "")
        for (pattern in patterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(cString).find()) return pattern
        }
        return null
    }

    private fun sanitizeFancyChat(toClean: String): String {
        return toClean.replace(Regex("[^\\u0000-\\u007F]"), "")
    }

    private fun sendResult(name: String, message: String) {
        if (showBlocked == ShowBlocked.CHAT || showBlocked == ShowBlocked.BOTH) {
            NoSpamMessage.sendMessage("${getChatName()} $name: $message")
        }
        if (showBlocked == ShowBlocked.LOG_FILE || showBlocked == ShowBlocked.BOTH) {
            MetaMod.logger.info("${getChatName()} $name: $message")
        }
    }

    private enum class Mode { REPLACE, HIDE }
    private enum class Page { TYPE, SETTINGS }
    private enum class ReplaceMode(val redaction: String) { REDACTED("[redacted]"), ASTERISKS("****") }
    private enum class ShowBlocked { NONE, LOG_FILE, CHAT, BOTH }
}
