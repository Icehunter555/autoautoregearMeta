package dev.wizard.meta.gui.clickgui.component

import dev.wizard.meta.gui.clickgui.TrollClickGui
import dev.wizard.meta.gui.clickgui.window.ModuleSettingWindow
import dev.wizard.meta.gui.rgui.component.Button
import dev.wizard.meta.module.AbstractModule

class ModuleButton(
    override val screen: TrollClickGui,
    val module: AbstractModule
) : Button(screen, module.name, module.description) {

    private val settingWindow by lazy { ModuleSettingWindow(screen, module) }

    init {
        action { _, buttonId ->
            when (buttonId) {
                0 -> module.toggle()
                1 -> screen.displayWindow(settingWindow)
            }
        }
    }

    override val progress: Float
        get() = if (module.isEnabled) 1.0f else 0.0f
}
