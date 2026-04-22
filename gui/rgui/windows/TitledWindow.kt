package dev.wizard.meta.gui.rgui.windows

import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.util.text.format
import net.minecraft.util.text.TextFormatting

open class TitledWindow(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: Component.UiSettingGroup
) : BasicWindow(screen, name, uiSettingGroup) {

    override val draggableHeight get() = MainFontRenderer.height + 6.0f
    override val minimizable = true

    override fun onRender(absolutePos: Long) {
        super.onRender(absolutePos)
        MainFontRenderer.drawString(TextFormatting.BOLD.format(name), 3.0f, 3.5f, ClickGUI.text)
    }
}
