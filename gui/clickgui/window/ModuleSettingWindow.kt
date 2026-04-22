package dev.wizard.meta.gui.clickgui.window

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.windows.SettingWindow
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.setting.groups.SettingGroup
import dev.wizard.meta.setting.settings.AbstractSetting

class ModuleSettingWindow(
    screen: IGuiScreen,
    module: AbstractModule
) : SettingWindow<AbstractModule>(screen, module.name, module, Component.UiSettingGroup.NONE) {

    override val elementSettingGroup: SettingGroup
        get() = element.settingGroup

    override val elementSettingList: List<AbstractSetting<*>>
        get() = element.fullSettingList.filter { it.name != "Enabled" }
}
