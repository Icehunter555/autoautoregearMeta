package dev.wizard.meta.gui.hudgui.component

import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.TrollHudGui
import dev.wizard.meta.gui.hudgui.window.HudSettingWindow
import dev.wizard.meta.gui.rgui.component.Button

class HudButton(
    override val screen: TrollHudGui,
    val hudElement: AbstractHudElement
) : Button(screen, hudElement.name, hudElement.description) {

    private val settingWindow by lazy { HudSettingWindow(screen, hudElement) }

    init {
        action { _, buttonId ->
            when (buttonId) {
                0 -> hudElement.visible = !hudElement.visible
                1 -> screen.displayWindow(settingWindow)
            }
        }
    }

    override val progress: Float
        get() = if (hudElement.visible) 1.0f else 0.0f
}
