package dev.wizard.meta.module.modules.client

import com.google.gson.reflect.TypeToken
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.GuiEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.gui.AbstractTrollGui
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.setting.ConfigManager
import dev.wizard.meta.setting.GenericConfig
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.ModuleConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.setting.configs.IConfig
import dev.wizard.meta.setting.settings.impl.collection.MapSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.translation.TranslationManager
import dev.wizard.meta.util.ConfigUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.EnumTextColor
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.TextFormattingKt
import dev.wizard.meta.util.threads.DefaultScope
import dev.wizard.meta.util.threads.TimerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiMainMenu
import java.awt.Color
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*

object Settings : AbstractModule(
    "Settings",
    category = Category.CLIENT,
    description = "Various settings to configure the client",
    alwaysEnabled = true,
    config = GenericConfig
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.CONFIG))
    const val defaultPreset = "default"

    val messageDelay by setting(this, IntegerSetting(settingName("Chat Message Delay"), 500, 0..20000, 50, { page == Page.CHAT }))
    val maxMessageQueueSize by setting(this, IntegerSetting(settingName("Max Message Queue Size"), 50, 10..200, 5, { page == Page.CHAT }))
    var prefix by setting(this, StringSetting(settingName("Prefix"), ";", { page == Page.CHAT }))

    val clientMessagePrefix by setting(this, EnumSetting(settingName("Client Message Prefix"), ClientMessageText.UPPERCASEM, { page == Page.CHAT }))
    val clientMessageBrackets by setting(this, EnumSetting(settingName("Client Message Brackets"), ClientMessageBrackets.SQUARE, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessagePrefixColor by setting(this, EnumSetting(settingName("Client Message Prefix Color"), EnumTextColor.LIGHT_PURPLE, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE && !customPrefixColor }))
    val customPrefixColor by setting(this, BooleanSetting(settingName("Custom Prefix Color"), false, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE }))
    val customPrefixColorRgb by setting(this, ColorSetting(settingName("Prefix Color"), ColorRGB(255, 140, 180), { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE && customPrefixColor }))
    val clientMessagePrefixWarningColor by setting(this, EnumSetting(settingName("Client Message Prefix Warning Color"), EnumTextColor.GOLD, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessagePrefixErrorColor by setting(this, EnumSetting(settingName("Client Message Prefix Error Color"), EnumTextColor.DARK_RED, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessageBracketColor by setting(this, EnumSetting(settingName("Client Message Bracket Color"), EnumTextColor.WHITE, { page == Page.CHAT && clientMessageBrackets != ClientMessageBrackets.NONE && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessagePrefixBold by setting(this, BooleanSetting(settingName("Client Message Prefix Bold"), false, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessageBracketsBold by setting(this, BooleanSetting(settingName("Client Message Brackets Bold"), false, { page == Page.CHAT && clientMessageBrackets != ClientMessageBrackets.NONE && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessagePrefixUnderline by setting(this, BooleanSetting(settingName("Client Message Prefix Underline"), false, { page == Page.CHAT && clientMessagePrefix != ClientMessageText.NONE }))
    val clientMessageBracketsUnderline by setting(this, BooleanSetting(settingName("Client Message Brackets Underline"), false, { page == Page.CHAT && clientMessageBrackets != ClientMessageBrackets.NONE && clientMessagePrefix != ClientMessageText.NONE }))

    val autoSaving by setting(this, BooleanSetting(settingName("Auto Saving"), true, { page == Page.CONFIG }))
    val savingFeedBack by setting(this, BooleanSetting(settingName("Saving FeedBack"), false, { autoSaving && page == Page.CONFIG }))
    val savingInterval by setting(this, IntegerSetting(settingName("Interval"), 10, 1..30, 1, { autoSaving && page == Page.CONFIG }, "Frequency of auto saving in minutes"))
    val serverPreset by setting(this, BooleanSetting(settingName("Server Preset"), false, { page == Page.CONFIG }))
    val guiPresetSetting = setting(this, StringSetting(settingName("Gui Preset"), defaultPreset, { page == Page.CONFIG }))
    val modulePresetSetting = setting(this, StringSetting(settingName("Module Preset"), defaultPreset, { page == Page.CONFIG }))

    val overrideLanguage = setting(this, BooleanSetting(settingName("Override Language"), false, { page == Page.LANGUAGE }))
    val language = setting(this, StringSetting(settingName("Language"), "en_us", { overrideLanguage.value && page == Page.LANGUAGE }))

    private val timer = TickTimer(TimeUnit.MINUTES)
    private var connected = false

    init {
        TimerScope.launchLooping("Config Auto Saving", 60000L) {
            if (autoSaving && mc.currentScreen !is AbstractTrollGui && timer.tickAndReset(savingInterval.value.toLong())) {
                if (savingFeedBack) {
                    NoSpamMessage.sendMessage("Auto saving settings...")
                } else {
                    MetaMod.logger.info("Auto saving settings...")
                }
                ConfigUtils.saveAll()
            }
        }

        listener<GuiEvent.Displayed>(priority = 114514) {
            if (it.screen is GuiMainMenu) {
                TranslationManager.reload()
            }
        }

        listener<ConnectionEvent.Connect> {
            connected = true
        }

        safeListener<TickEvent.Pre> {
            if (serverPreset && connected && !mc.isSingleplayer) {
                val serverData = mc.currentServerData
                val ip = serverData?.serverIP ?: return@safeListener
                connected = false
                ConfigType.GUI.setServerPreset(ip)
                ConfigType.MODULES.setServerPreset(ip)
            } else {
                connected = false
            }
        }

        val presetConsumer: (String, String) -> String = { prev, input ->
            if (verifyPresetName(input)) input else if (verifyPresetName(prev)) prev else defaultPreset
        }
        guiPresetSetting.consumers.add(presetConsumer)
        modulePresetSetting.consumers.add(presetConsumer)

        overrideLanguage.listeners.add { TranslationManager.reload() }
        language.listeners.add { TranslationManager.reload() }
    }

    private fun verifyPresetName(input: String): Boolean {
        val nameWithoutExtension = input.removeSuffix(".json")
        val nameWithExtension = "$nameWithoutExtension.json"
        return if (!ConfigUtils.isPathValid(nameWithExtension)) {
            NoSpamMessage.sendMessage("${TextFormattingKt.formatValue(nameWithoutExtension)} is not a valid preset name")
            false
        } else {
            true
        }
    }

    fun updatePreset(setting: StringSetting, input: String, config: IConfig) {
        if (!verifyPresetName(input)) return
        val nameWithoutExtension = input.removeSuffix(".json")
        val prev = setting.value
        try {
            ConfigManager.save(config)
            setting.value = nameWithoutExtension
            ConfigManager.save(GenericConfig)
            ConfigManager.load(config)
            NoSpamMessage.sendMessage("Preset set to ${TextFormattingKt.formatValue(nameWithoutExtension)}!")
        } catch (e: IOException) {
            NoSpamMessage.sendMessage("Couldn't set preset: ${e.message}")
            MetaMod.logger.warn("Couldn't set path!", e)
            setting.value = prev
            ConfigManager.save(GenericConfig)
        }
    }

    fun getSettingLanguage(): String {
        return if (overrideLanguage.value) language.value else Wrapper.getMinecraft().gameSettings.language ?: "en_us"
    }

    @JvmStatic
    fun getChatColor(): Color {
        val color = customPrefixColorRgb
        return Color(color.r, color.g, color.b, 255)
    }

    enum class ClientMessageBrackets(override val displayName: CharSequence) : DisplayEnum {
        SQUARE("Square []"), CURLY("Curly {}"), NORMAL("Normal ()"), ANGLED("Angled <>"),
        DOUBLEANGLED("Double Angled <<>>"), CORNER("Corner Brackets"), ROUNDED_BLOCK("Rounded Block"), NONE("None")
    }

    enum class ClientMessageText(override val displayName: CharSequence) : DisplayEnum {
        UPPERCASEM("M"), LOWERCASEM("m"), UPPERCASEMETA("BETA"), CAPITALIZEDMETA("Meta"),
        LOWERCASEMETA("meta"), CIRCLEM("M (with circle)"), ARROW("->"), COMMAND(">_"), HASHTAG("#"), NONE("None")
    }

    enum class ConfigType(
        override val displayName: CharSequence,
        val config: AbstractConfig<*>,
        val setting: StringSetting
    ) : DisplayEnum, IConfigType {
        GUI("GUI", GuiConfig, guiPresetSetting),
        MODULES("Modules", ModuleConfig, modulePresetSetting);

        override fun getConfig(): AbstractConfig<*> = config
        override fun getSetting(): StringSetting = setting
        override fun getServerPresets(): Set<String> = Companion.getJsons(config.filePath) { it.name.startsWith("server-") }
        override fun getAllPresets(): Set<String> = Companion.getJsons(config.filePath) { true }

        companion object {
            fun getJsons(path: String, filter: (File) -> Boolean): Set<String> {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) return emptySet()
                val files = dir.listFiles() ?: return emptySet()
                return files.filter { it.isFile && it.extension == "json" && it.length() > 8 && filter(it) }
                    .map { it.nameWithoutExtension }
                    .toSet()
            }
        }
    }

    interface IConfigType : DisplayEnum {
        fun getConfig(): AbstractConfig<*>
        fun getSetting(): StringSetting
        fun getServerPresets(): Set<String>
        fun getAllPresets(): Set<String>

        fun reload() {
            DefaultScope.launch(Dispatchers.IO) {
                var loaded = ConfigManager.load(GenericConfig)
                loaded = ConfigManager.load(getConfig()) || loaded
                if (loaded) {
                    NoSpamMessage.sendMessage("${TextFormattingKt.formatValue(getConfig().name)} config reloaded!")
                } else {
                    NoSpamMessage.sendError("Failed to load ${TextFormattingKt.formatValue(getConfig().name)} config!")
                }
            }
        }

        fun save() {
            DefaultScope.launch(Dispatchers.IO) {
                var saved = ConfigManager.save(GenericConfig)
                saved = ConfigManager.save(getConfig()) || saved
                if (saved) {
                    NoSpamMessage.sendMessage("${TextFormattingKt.formatValue(getConfig().name)} config saved!")
                } else {
                    NoSpamMessage.sendError("Failed to load ${TextFormattingKt.formatValue(getConfig().name)} config!")
                }
            }
        }

        fun setPreset(name: String) {
            DefaultScope.launch(Dispatchers.IO) {
                updatePreset(getSetting(), name, getConfig())
            }
        }

        fun copyPreset(name: String) {
            DefaultScope.launch(Dispatchers.IO) {
                if (name == getSetting().value) {
                    NoSpamMessage.sendError("Destination preset name ${TextFormattingKt.formatValue(name)} is same as current preset")
                }
                ConfigManager.save(getConfig())
                try {
                    val fileFrom = File("${getConfig().filePath}/${getSetting().value}.json")
                    val fileTo = File("${getConfig().filePath}/$name.json")
                    fileFrom.copyTo(fileTo, overwrite = true)
                } catch (e: Exception) {
                    NoSpamMessage.sendError("Failed to copy preset, ${e.message}")
                    MetaMod.logger.error("Failed to copy preset", e)
                }
            }
        }

        fun deletePreset(name: String) {
            DefaultScope.launch(Dispatchers.IO) {
                if (!getAllPresets().contains(name)) {
                    NoSpamMessage.sendMessage("${TextFormattingKt.formatValue(name)} is not a valid preset for ${TextFormattingKt.formatValue(displayName)} config")
                    return@launch
                }
                try {
                    val file = File("${getConfig().filePath}/$name.json")
                    val fileBak = File("${getConfig().filePath}/$name.bak")
                    file.delete()
                    fileBak.delete()
                    NoSpamMessage.sendMessage("Deleted preset $name for ${TextFormattingKt.formatValue(displayName)} config")
                } catch (e: Exception) {
                    NoSpamMessage.sendError("Failed to delete preset, ${e.message}")
                    MetaMod.logger.error("Failed to delete preset", e)
                }
            }
        }

        fun printCurrentPreset() {
            val path = Paths.get("${getConfig().filePath}/${getSetting().value}.json").toAbsolutePath()
            NoSpamMessage.sendMessage("Path to config: ${TextFormattingKt.formatValue(path)}")
        }

        fun printAllPresets() {
            if (getAllPresets().isEmpty()) {
                NoSpamMessage.sendMessage("No preset for ${TextFormattingKt.formatValue(displayName)} config!")
            } else {
                val sb = StringBuilder("List of presets: ${TextFormattingKt.formatValue(getAllPresets().size)}\n")
                getAllPresets().forEach {
                    val path = Paths.get("${getConfig().filePath}/$it.json").toAbsolutePath()
                    sb.append(TextFormattingKt.formatValue(path)).append("\n")
                }
                NoSpamMessage.sendMessage(sb.toString())
            }
        }

        fun newServerPreset(ip: String) {
            if (!serverPresetDisabledMessage()) return
            setPreset(convertIpToPresetName(ip))
        }

        fun setServerPreset(ip: String) {
            if (!serverPresetDisabledMessage()) return
            val presetName = convertIpToPresetName(ip)
            if (getServerPresets().contains(presetName)) {
                NoSpamMessage.sendMessage("Changing preset to ${TextFormattingKt.formatValue(presetName)} for ${TextFormattingKt.formatValue(displayName)} config")
                setPreset(presetName)
            } else {
                NoSpamMessage.sendMessage("No server preset found for ${TextFormattingKt.formatValue(displayName)} config, using ${TextFormattingKt.formatValue(defaultPreset)} preset...")
                setPreset(defaultPreset)
            }
        }

        fun deleteServerPreset(ip: String) {
            deletePreset(convertIpToPresetName(ip))
        }

        fun printAllServerPreset() {
            if (!serverPresetDisabledMessage()) return
            if (getServerPresets().isEmpty()) {
                NoSpamMessage.sendMessage("No server preset for ${TextFormattingKt.formatValue(displayName)} config!")
            } else {
                val sb = StringBuilder("List of server presets for ${TextFormattingKt.formatValue(displayName)} config: ${TextFormattingKt.formatValue(getServerPresets().size)}\n")
                getServerPresets().forEach {
                    val path = Paths.get("${getConfig().filePath}/$it.json").toAbsolutePath()
                    sb.append(TextFormattingKt.formatValue(path)).append("\n")
                }
                NoSpamMessage.sendMessage(sb.toString())
            }
        }

        private fun convertIpToPresetName(ip: String): String {
            return "server-" + ip.replace('.', '_').replace(':', '_')
        }

        private fun serverPresetDisabledMessage(): Boolean {
            return if (!serverPreset) {
                NoSpamMessage.sendMessage("Server preset is not enabled, enable it in Configurations in ClickGUI")
                false
            } else {
                true
            }
        }
    }

    private enum class Page { LANGUAGE, CONFIG, CHAT }
}
