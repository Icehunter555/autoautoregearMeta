package dev.wizard.meta.gui.hudgui

import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.settings.SettingRegister

abstract class LabelHud(
    name: String,
    alias: Array<out String> = emptyArray(),
    category: Category,
    description: String,
    alwaysListening: Boolean = false,
    enabledByDefault: Boolean = false
) : AbstractLabelHud(name, alias, category, description, alwaysListening, enabledByDefault, GuiConfig), SettingRegister<Component> by GuiConfig
