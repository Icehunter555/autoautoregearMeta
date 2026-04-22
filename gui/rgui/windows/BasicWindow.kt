package dev.wizard.meta.gui.rgui.windows

import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.shaders.WindowBlurShader
import dev.wizard.meta.gui.IGuiScreen
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.util.interfaces.Nameable

open class BasicWindow(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: Component.UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : CleanWindow(name, screen, uiSettingGroup, config) {

    override fun onRender(absolutePos: Long) {
        super.onRender(absolutePos)
        WindowBlurShader.render(renderWidth, renderHeight)
        if (ClickGUI.titleBar) {
            RenderUtils2D.drawBottomRoundedRectFilled(0.0f, draggableHeight, renderWidth, renderHeight - draggableHeight, ClickGUI.radius, ClickGUI.backGround, ClickGUI.roundSegments)
            RenderUtils2D.drawTopRoundedRectFilled(0.0f, 0.0f, renderWidth, draggableHeight, ClickGUI.radius, ClickGUI.primary, ClickGUI.roundSegments)
        } else {
            if (ClickGUI.line) {
                RenderUtils2D.drawAnimatedGradientLine(0.0f, 15.0f, renderWidth, 15.0f, ClickGUI.lineWidth, ClickGUI.primary, ColorRGB(255, 255, 255), 1.0f, true, 50)
            }
            RenderUtils2D.drawRoundedRectFilled(0.0f, 0.0f, renderWidth, renderHeight, ClickGUI.radius, ClickGUI.backGround, ClickGUI.roundSegments)
        }
        if (ClickGUI.windowOutline) {
            RenderUtils2D.drawRoundedRectOutline(0.0f, 0.0f, renderWidth, renderHeight, ClickGUI.radius, 1.0f, ColorRGB.alpha(ClickGUI.primary, 255), ClickGUI.roundSegments)
        }
    }
}
