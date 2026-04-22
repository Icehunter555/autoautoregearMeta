package dev.wizard.meta.gui.rgui.windows

import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.util.interfaces.Nameable

open class CleanWindow(
    name: CharSequence,
    screen: IGuiScreen,
    uiSettingGroup: Component.UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : WindowComponent(screen, name, uiSettingGroup, config)
