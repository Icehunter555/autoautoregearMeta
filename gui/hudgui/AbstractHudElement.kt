package dev.wizard.meta.gui.hudgui

import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.HAlign
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.hudgui.elements.hud.Watermark
import dev.wizard.meta.gui.rgui.Component
import dev.wizard.meta.gui.rgui.windows.BasicWindow
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.GuiConfig
import dev.wizard.meta.setting.configs.AbstractConfig
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.translation.ITranslateSrc
import dev.wizard.meta.translation.TranslateSrc
import dev.wizard.meta.translation.TranslateType
import dev.wizard.meta.translation.TranslationKey
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.interfaces.Alias
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.interfaces.Nameable
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.client.renderer.GlStateManager

abstract class AbstractHudElement(
    name: String,
    override val alias: Array<out CharSequence>,
    val category: Category,
    val description: String,
    val alwaysListening: Boolean,
    enabledByDefault: Boolean,
    config: AbstractConfig<out Nameable>
) : BasicWindow(TrollHudGui, name, Component.UiSettingGroup.HUD_GUI, config), Alias, IListenerOwner by ListenerOwner() {

    val bind by GuiConfig.setting(this, "Bind", Bind())
    var scale by GuiConfig.setting(this, "Scale", 1.0f, 0.1f..4.0f, 0.05f)
    val default = GuiConfig.setting(this, "Default", false, isTransient = true)

    override val resizable = false
    override val minWidth by FrameFloat { MainFontRenderer.height * scale * 2.0f }
    override val minHeight by FrameFloat { MainFontRenderer.height * scale }

    override val maxWidth get() = hudWidth * scale
    override val maxHeight get() = hudHeight * scale

    open val hudWidth: Float get() = 20.0f
    open val hudHeight: Float get() = 10.0f

    val settingList get() = GuiConfig.getGroupOrPut(Component.UiSettingGroup.HUD_GUI.groupName).getGroupOrPut(internalName).settings

    init {
        parallelListener<TickEvent.Pre> {
            if (visible) {
                width = maxWidth
                height = maxHeight
            }
        }

        visibleSetting.valueListeners.add { _, it ->
            if (it) {
                subscribe()
                lastActiveTime = System.currentTimeMillis()
            } else if (!alwaysListening) {
                unsubscribe()
            }
        }

        default.valueListeners.add { _, it ->
            if (it) {
                settingList.filter { it != visibleSetting && it != default }.forEach { it.resetValue() }
                default.value = false
                NoSpamMessage.sendMessage(Companion, "$name $defaultMessage!")
            }
        }

        if (!enabledByDefault) {
            visible = false
        }
    }

    override fun onDisplayed() {
        super.onDisplayed()
        if (alwaysListening || visible) subscribe()
    }

    override fun onClosed() {
        super.onClosed()
        if (alwaysListening || visible) subscribe()
    }

    override fun onTick() {
        super.onTick()
    }

    override fun onRelease(mousePos: Long, clickPos: Long, buttonId: Int) {
        super.onRelease(mousePos, clickPos, buttonId)
        if (buttonId == 2) {
            TrollHudGui.searchString = nameAsString
        }
    }

    override fun onRender(absolutePos: Long) {
        renderFrame()
        GlStateManager.scale(scale, scale, scale)
        renderHud()
    }

    open fun renderHud() {}

    open fun renderFrame() {
        RenderUtils2D.drawRectFilled(0.0f, 0.0f, renderWidth, renderHeight, ClickGUI.backGround)
        if (ClickGUI.windowOutline) {
            RenderUtils2D.drawRectOutline(0.0f, 0.0f, renderWidth, renderHeight, 1.0f, ClickGUI.primary)
        }
    }

    fun renderCapsuleFrame(
        xPadding: Float,
        yPadding: Float,
        segments: Int = 55,
        bottomLine: Boolean = false,
        topLine: Boolean = false,
        lineWidth: Float = 1.0f,
        lineSegments: Int = 50,
        lineColor1: Int = ClickGUI.primary,
        lineColor2: Int = ColorRGB(255, 255, 255),
        lineSpeed: Float = 1.0f
    ) {
        val x = -xPadding
        val y = -yPadding
        val x2 = renderWidth + xPadding + 7.0f
        val y2 = renderHeight + yPadding

        when (dockingH) {
            HAlign.LEFT -> RenderUtils2D.drawRightCapsuleRectFilled(x, y, x2, y2, ClickGUI.backGround, segments)
            HAlign.RIGHT -> RenderUtils2D.drawLeftCapsuleRectFilled(x, y, x2, y2, ClickGUI.backGround, segments)
            HAlign.CENTER -> RenderUtils2D.drawCapsuleRectFilled(x, y, x2, y2, ClickGUI.backGround, segments)
        }

        val circle = Math.min(x2 / 2.0f, y2 / 2.0f)
        if (topLine) {
            when (dockingH) {
                HAlign.LEFT -> RenderUtils2D.drawAnimatedGradientLine(x - 4.0f, y, x2 - circle - 4.0f, y, lineWidth, lineColor1, lineColor2, lineSpeed, true, lineSegments)
                HAlign.RIGHT -> RenderUtils2D.drawAnimatedGradientLine(x + circle, y, x2, y, lineWidth, lineColor1, lineColor2, lineSpeed, true, lineSegments)
                HAlign.CENTER -> RenderUtils2D.drawAnimatedGradientLine(x + circle, y, x2 - circle, y, lineWidth, lineColor1, lineColor2, lineSpeed, true, lineSegments)
            }
        }
        if (bottomLine) {
            when (dockingH) {
                HAlign.LEFT -> RenderUtils2D.drawAnimatedGradientLine(x - 4.0f, y2, x2 - circle - 4.0f, y2, lineWidth, lineColor1, lineColor2, lineSpeed, true, lineSegments)
                HAlign.RIGHT -> RenderUtils2D.drawAnimatedGradientLine(x - 4.0f + circle, y2, x2 - 4.0f, y2, lineWidth, lineColor1, lineColor2, lineSpeed, true, lineSegments)
                HAlign.CENTER -> RenderUtils2D.drawAnimatedGradientLine(x - 4.0f + circle, y2, x2 - circle - 4.0f, y2, lineWidth, lineColor1, lineColor2, lineSpeed, true, lineSegments)
            }
        }
    }

    fun renderFrame(wpadding: Float, hpadding: Float, rad: Float = 1.6f) {
        val x = -wpadding
        val y = -hpadding
        val x2 = renderWidth + wpadding
        val y2 = renderHeight + hpadding
        RenderUtils2D.drawRoundedRectFilled(x, y, x2, y2, rad, ClickGUI.backGround)
        if (ClickGUI.windowOutline) {
            RenderUtils2D.drawRoundedRectOutline(x, y, x2, y2, 1.0f, rad, ClickGUI.primary)
        }
    }

    fun renderWatermarkFrame(
        wpadding: Float,
        hpadding: Float,
        segments: Int = 55,
        line: Boolean = false,
        lineWidth: Float = 1.0f,
        lineSegments: Int = 50
    ) {
        val x = -wpadding
        val y = -hpadding
        val x2 = renderWidth + wpadding
        val y2 = renderHeight + hpadding
        RenderUtils2D.drawRightCapsuleRectFilled(x, y, x2, y2, ClickGUI.backGround, segments)
        val minus = Math.min(x2 / 2.0f, y2 / 2.0f)
        if (line) {
            RenderUtils2D.drawAnimatedGradientLine(x, y2, x2 - minus, y2, lineWidth, Watermark.color1, Watermark.color2, Watermark.gradientSpeed, true, lineSegments)
        }
    }

    enum class Category(override val displayName: CharSequence) : DisplayEnum {
        TEXT("Text"),
        HUD("Hud")
    }

    enum class Side(override val displayName: CharSequence) : DisplayEnum {
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right")
    }

    companion object : ITranslateSrc by TranslateSrc("hud") {
        val defaultMessage = key(TranslateType.COMMON, "setToDefault" to "Set to defaults")
    }
}
