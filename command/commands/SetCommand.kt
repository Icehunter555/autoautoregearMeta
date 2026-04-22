package dev.wizard.meta.command.commands

import dev.fastmc.common.TimeUnit
import dev.wizard.meta.MetaMod
import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.gui.GuiManager
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.delegate.AsyncCachedValue
import dev.wizard.meta.util.extension.remove
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import net.minecraft.util.text.TextFormatting
import java.util.*

object SetCommand : ClientCommand("set", arrayOf("setting", "settings"), "Change the setting of a certain module.") {

    private val moduleSettingMap: Map<AbstractModule, Map<String, AbstractSetting<*>>> by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        ModuleManager.modules.associateWith { module ->
            module.fullSettingList.associateBy { formatSetting(it.nameAsString, false) }
        }
    }

    private val hudElementSettingMap: Map<AbstractHudElement, Map<String, AbstractSetting<*>>> by AsyncCachedValue(5L, TimeUnit.SECONDS) {
        GuiManager.hudElements.associateWith { hud ->
            hud.settingList.associateBy { formatSetting(it.nameAsString, false) }
        }
    }

    private fun formatSetting(str: String, lowerCase: Boolean = true): String {
        val sanitized = str.remove(' ', '_')
        return if (lowerCase) sanitized.lowercase(Locale.ROOT) else sanitized
    }

    private fun getSetting(module: AbstractModule, settingName: String): AbstractSetting<*>? {
        return moduleSettingMap[module]?.get(formatSetting(settingName, false))
    }

    private fun getSetting(hud: AbstractHudElement, settingName: String): AbstractSetting<*>? {
        return hudElementSettingMap[hud]?.get(formatSetting(settingName, false))
    }

    private fun toggleSetting(name: String, settingName: String, setting: AbstractSetting<*>?) {
        if (setting == null) {
            sendUnknownSettingMessage(settingName, name)
            return
        }
        when (setting) {
            is BooleanSetting -> setting.value = !setting.value
            is EnumSetting<*> -> setting.nextValue()
            else -> NoSpamMessage.sendMessage("Unable to toggle value for ${setting.name.formatValue()}")
        }
        NoSpamMessage.sendMessage("Set ${setting.name.formatValue()} to ${setting.value.formatValue()}.")
    }

    private fun setSetting(name: String, settingName: String, setting: AbstractSetting<*>?, value: String) {
        if (setting == null) {
            sendUnknownSettingMessage(settingName, name)
            return
        }
        try {
            setting.setValue(value)
            NoSpamMessage.sendMessage("Set ${setting.name.formatValue()} to ${value.formatValue()}.")
        } catch (e: Exception) {
            NoSpamMessage.sendMessage("Unable to set value! ${TextFormatting.RED}${e.message}")
            MetaMod.logger.info("Unable to set value!", e)
        }
    }

    private fun printSetting(name: String, settingName: String, setting: AbstractSetting<*>?) {
        if (setting == null) {
            sendUnknownSettingMessage(settingName, name)
            return
        }
        NoSpamMessage.sendMessage("${settingName.formatValue()} is a ${setting.valueClass.simpleName.formatValue()}. Its current value is ${setting.formatValue()}")
    }

    private fun listSetting(name: String, settingList: List<AbstractSetting<*>>) {
        val stringBuilder = StringBuilder("List of settings for ${name.formatValue()} ${settingList.size.formatValue()}\n")
        settingList.forEach {
            stringBuilder.append("    ${formatSetting(it.nameAsString, false)} ${TextFormatting.GRAY}${it.value}\n")
        }
        NoSpamMessage.sendMessage(stringBuilder.toString())
    }

    private fun sendUnknownSettingMessage(settingName: String, name: String) {
        NoSpamMessage.sendMessage("Unknown setting ${settingName.formatValue()} in ${name.formatValue()}!")
    }

    init {
        hudElement("hud element") { hudElementArg ->
            string("setting") { settingArg ->
                literal("toggle") {
                    execute {
                        val hud = getValue(hudElementArg)
                        val settingName = getValue(settingArg)
                        val setting = getSetting(hud, settingName)
                        toggleSetting(hud.nameAsString, settingName, setting)
                    }
                }

                greedy("value") { valueArg ->
                    execute("Set the value of a hud element's setting") {
                        val hud = getValue(hudElementArg)
                        val settingName = getValue(settingArg)
                        val setting = getSetting(hud, settingName)
                        setSetting(hud.nameAsString, settingName, setting, getValue(valueArg))
                    }
                }

                execute("Show the value of a setting") {
                    val hud = getValue(hudElementArg)
                    val settingName = getValue(settingArg)
                    val setting = getSetting(hud, settingName)
                    printSetting(hud.nameAsString, settingName, setting)
                }
            }

            execute("List settings for a hud element") {
                val hud = getValue(hudElementArg)
                listSetting(hud.nameAsString, hud.settingList)
            }
        }

        module("module") { moduleArg ->
            string("setting") { settingArg ->
                literal("toggle") {
                    execute {
                        val module = getValue(moduleArg)
                        val settingName = getValue(settingArg)
                        val setting = getSetting(module, settingName)
                        toggleSetting(module.nameAsString, settingName, setting)
                    }
                }

                greedy("value") { valueArg ->
                    execute("Set the value of a module's setting") {
                        val module = getValue(moduleArg)
                        val settingName = getValue(settingArg)
                        val setting = getSetting(module, settingName)
                        setSetting(module.nameAsString, settingName, setting, getValue(valueArg))
                    }
                }

                execute("Show the value of a setting") {
                    val module = getValue(moduleArg)
                    val settingName = getValue(settingArg)
                    val setting = getSetting(module, settingName)
                    printSetting(module.nameAsString, settingName, setting)
                }
            }

            execute("List settings for a module") {
                val module = getValue(moduleArg)
                listSetting(module.nameAsString, module.fullSettingList)
            }
        }
    }
}
