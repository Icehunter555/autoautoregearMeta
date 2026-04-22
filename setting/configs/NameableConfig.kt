package dev.wizard.meta.setting.configs

import dev.wizard.meta.setting.settings.AbstractSetting
import dev.wizard.meta.util.interfaces.Nameable

open class NameableConfig<T : Nameable>(name: String, filePath: String) : AbstractConfig<T>(name, filePath) {
    override fun addSettingToConfig(owner: T, setting: AbstractSetting<*>) {
        getGroupOrPut(owner.internalName).addSetting(setting)
    }

    open fun getSettings(nameable: Nameable): List<AbstractSetting<*>> {
        return getGroup(nameable.internalName)?.settings ?: emptyList()
    }
}
