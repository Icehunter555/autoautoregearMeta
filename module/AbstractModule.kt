package dev.wizard.meta.module

import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.ListenerOwner
import dev.wizard.meta.event.events.ModuleToggleEvent
import dev.wizard.meta.manager.managers.MetaManager
import dev.wizard.meta.setting.configs.NameableConfig
import dev.wizard.meta.setting.groups.SettingMultiGroup
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.setting.settings.SettingRegister
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.translation.ITranslateSrc
import dev.wizard.meta.translation.TranslateSrc
import dev.wizard.meta.translation.TranslateType
import dev.wizard.meta.translation.TranslationKey
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.IDRegistry
import dev.wizard.meta.util.interfaces.Alias
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.interfaces.Nameable
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.client.Minecraft

open class AbstractModule(
    name: String,
    alias: Array<String> = emptyArray(),
    val category: Category,
    description: String,
    val modulePriority: Int = -1,
    var alwaysListening: Boolean = false,
    visible: Boolean = true,
    devOnly: Boolean = false,
    val alwaysEnabled: Boolean = false,
    val enabledByDefault: Boolean = false,
    private val config: NameableConfig<out Nameable>
) : ListenerOwner(),
    Nameable,
    Alias,
    SettingRegister<Nameable>,
    ITranslateSrc by TranslateSrc("module_" + name.replace(" ", "_")),
    Comparable<AbstractModule> {

    final override val internalName: String = name.replace(" ", "")
    final override val name: TranslationKey = key(TranslateType.SPECIFIC, "name" to name)
    override val alias: Array<String> = arrayOf(internalName, *alias)
    val description: TranslationKey = key(TranslateType.SPECIFIC, "description" to description)
    val id = idRegistry.register()

    val enabled: BooleanSetting = setting(this, BooleanSetting(settingName("Enabled"), false, { false }))
    val bind: BindSetting = setting(this, BindSetting(settingName("Bind"), Bind(), { !alwaysEnabled }, {
        when (onHold.value) {
            OnHold.OFF -> if (it) toggle()
            OnHold.ENABLE -> toggle(it)
            OnHold.DISABLE -> toggle(!it)
        }
    }))
    private val onHold: EnumSetting<OnHold> = setting(this, EnumSetting(settingName("On Hold"), OnHold.OFF, { !alwaysEnabled }))
    val visible: BooleanSetting = setting(this, BooleanSetting(settingName("Visible"), visible))
    val devOnly: BooleanSetting = setting(this, BooleanSetting(settingName("Hidden"), devOnly, { false }))
    val hudInfoDisplayMode: EnumSetting<HudDisplayInfo> = setting(this, EnumSetting(settingName("Display Info"), HudDisplayInfo.ENABLED, { visible }))
    val debug: BooleanSetting = setting(this, BooleanSetting(settingName("Debug"), false, { MetaManager.isDevorfalse() }))
    val default: BooleanSetting = setting(this, BooleanSetting(settingName("Default"), false, { settingList.isNotEmpty() }, isAction = true))

    init {
        enabled.consumers.add { prev, input ->
            val enabled = alwaysEnabled || input
            if (prev != input && !alwaysEnabled) {
                ModuleToggleEvent(this).post()
            }
            if (enabled || alwaysListening) {
                subscribe()
            } else {
                unsubscribe()
            }
            enabled
        }

        default.valueListeners.add { _, it ->
            if (it) {
                settingList.forEach { it.resetValue() }
                default.value = false
                NoSpamMessage.sendMessage(Companion, "${getChatName()} $defaultMessage!")
            }
        }
    }

    val settingGroup: SettingMultiGroup
        get() = config.getGroupOrPut(internalName)

    val fullSettingList: List<AbstractSetting<*>>
        get() = config.getSettings(this)

    val settingList: List<AbstractSetting<*>>
        get() = fullSettingList.filter {
            it != bind && it != enabled && it != visible && it != devOnly && it != default
        }

    fun isEnabled(): Boolean = enabled.value
    fun isDisabled(): Boolean = !isEnabled()

    fun getChatName(): String = "[$name]"

    fun isVisible(): Boolean = visible.value && !devOnly.value

    fun isDevOnly(): Boolean = devOnly.value

    fun getShowInfo(): Boolean = hudInfoDisplayMode.value == HudDisplayInfo.ENABLED

    private fun <T : AbstractSetting<*>> addSetting(setting: T) {
        @Suppress("UNCHECKED_CAST")
        (config as NameableConfig<Nameable>).addSettingToConfig(this, setting)
    }

    fun postInit() {
        enabled.value = enabledByDefault || alwaysEnabled
        if (alwaysListening) {
            subscribe()
        }
    }

    fun toggle(state: Boolean) {
        enabled.value = state
    }

    fun toggle() {
        enabled.value = !enabled.value
    }

    fun sendLog(message: String) {
        if (debug.value && MetaManager.isDevorfalse()) {
            MetaMod.logger.info("${getChatName()} [debug] $message")
        }
    }

    fun enable() {
        enabled.value = true
    }

    fun disable() {
        enabled.value = false
    }

    open fun isActive(): Boolean = isEnabled() || alwaysListening

    open fun getHudInfo(): String = ""

    protected fun onEnable(block: () -> Unit) {
        enabled.valueListeners.add { _, input ->
            if (input) block()
        }
    }

    protected fun onDisable(block: () -> Unit) {
        enabled.valueListeners.add { _, input ->
            if (!input) block()
        }
    }

    protected fun onToggle(block: (Boolean) -> Unit) {
        enabled.valueListeners.add { _, input ->
            block(input)
        }
    }

    override fun <S : AbstractSetting<*>> setting(nameable: Nameable, setting: S): S {
        @Suppress("UNCHECKED_CAST")
        (config as NameableConfig<Nameable>).addSettingToConfig(nameable, setting)
        return setting
    }

    override fun settingName(input: CharSequence): CharSequence {
        return if (input is String) key(TranslateType.COMMON, input) else input
    }

    override fun compareTo(other: AbstractModule): Int {
        val result = modulePriority.compareTo(other.modulePriority)
        if (result != 0) return result
        return id.compareTo(other.id)
    }

    enum class HudDisplayInfo(override val displayName: CharSequence) : DisplayEnum {
        ENABLED("On"),
        DISABLED("Off")
    }

    private enum class OnHold(override val displayName: CharSequence) : DisplayEnum {
        OFF(TranslateType.COMMON.commonKey("Off")),
        ENABLE(TranslateType.COMMON.commonKey("Enable")),
        DISABLE(TranslateType.COMMON.commonKey("Disable"))
    }

    companion object : ITranslateSrc by TranslateSrc("module") {
        val defaultMessage = key(TranslateType.COMMON, "setToDefault" to "Set to defaults")
        private val idRegistry = IDRegistry()
        val mc: Minecraft = Minecraft.getMinecraft()
    }
}
