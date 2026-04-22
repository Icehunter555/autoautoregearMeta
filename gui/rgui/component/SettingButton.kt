package dev.wizard.meta.gui.rgui.component

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting

class SettingButton(screen: IGuiScreen, val setting: BooleanSetting) : CheckButton(screen, setting.name, setting.description, setting.visibility) {

    override var state: Boolean
        get() = setting.value
        set(value) { setting.value = value }
}
