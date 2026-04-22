package dev.wizard.meta.gui.clickgui

import dev.wizard.meta.graphics.Resolution
import dev.wizard.meta.gui.AbstractTrollGui
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.clickgui.component.ModuleButton
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.WindowComponent
import dev.wizard.meta.gui.rgui.windows.ListWindow
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.util.extension.remove
import dev.wizard.meta.util.threads.runSynchronized
import java.util.*

object TrollClickGui : AbstractTrollGui() {
    override val alwaysTicking = false
    private val moduleWindows = EnumMap<Category, ListWindow>(Category::class.java)

    override var searchString: String
        get() = super.searchString
        set(value) {
            super.searchString = value
            val string = value.remove(' ')
            if (string.isNotEmpty()) {
                setModuleButtonVisibility { button ->
                    button.module.allNames.any { it.contains(string, ignoreCase = true) }
                }
            } else {
                setModuleButtonVisibility { true }
            }
        }

    init {
        var posX = 0.0f
        var posY = 0.0f
        for (category in Category.entries) {
            val window = ListWindow(this, category.displayName, Component.UiSettingGroup.CLICK_GUI)
            window.forcePosX = posX
            window.forcePosY = posY
            window.forceWidth = 80.0f
            window.forceHeight = 400.0f

            ModuleManager.modules.asSequence()
                .filter { it.category == category && !it.isDevOnly }
                .mapTo(window.children) { ModuleButton(this, it) }

            moduleWindows[category] = window
            posX += 80.0f
            if (posX > Resolution.trollWidthF) {
                posX = 0.0f
                posY += 300.0f
            }
        }

        windows.runSynchronized {
            addAll(moduleWindows.values)
        }
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        setModuleButtonVisibility { true }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1 || (keyCode == ClickGUI.bind.value.key && !searching)) {
            val lastClicked = lastClicked
            if (lastClicked?.keybordListening == null) {
                ClickGUI.disable()
                return
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    private inline fun setModuleButtonVisibility(function: (ModuleButton) -> Boolean) {
        moduleWindows.values.asSequence()
            .flatMap { it.children.asSequence() }
            .filterIsInstance<ModuleButton>()
            .forEach {
                it.visible = function(it)
            }
    }
}
