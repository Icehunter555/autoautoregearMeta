package dev.wizard.meta.gui.hudgui.window

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.windows.SettingWindow
import dev.wizard.meta.setting.groups.SettingGroup
import dev.wizard.meta.setting.settings.AbstractSetting

class HudSettingWindow(
    screen: IGuiScreen,
    hudElement: AbstractHudElement
) : SettingWindow<AbstractHudElement>(screen, hudElement.name, hudElement, Component.UiSettingGroup.NONE) {

    override val elementSettingGroup: SettingGroup
        get() = element.settingGroup

    override val elementSettingList: List<AbstractSetting<*>>
        get() = element.settingList
}
