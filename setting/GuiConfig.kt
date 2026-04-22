package dev.wizard.meta.setting

import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.setting.settings.AbstractSetting
import java.io.File

object GuiConfig : AbstractConfig<Component>("gui", "trollhack/config/gui") {
    override val file: File
        get() = File("$filePath/${Settings.INSTANCE.guiPreset}.json")

    override val backup: File
        get() = File("$filePath/${Settings.INSTANCE.guiPreset}.bak")

    override fun addSettingToConfig(owner: Component, setting: AbstractSetting<*>) {
        val groupName = owner.uiSettingGroup.groupName
        if (groupName.isNotEmpty()) {
            getGroupOrPut(groupName).getGroupOrPut(owner.internalName).addSetting(setting)
        }
    }
}
