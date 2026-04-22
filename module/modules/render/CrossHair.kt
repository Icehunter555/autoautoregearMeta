package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.gui.ScaledResolution

object CrossHair : Module(
    "CrossHair",
    category = Category.RENDER,
    description = "Custom Crosshair"
) {
    private val gapMode by setting(this, EnumSetting(settingName("Gap Mode"), GapMode.NORMAL))
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255)))

    private val lines by setting(this, BooleanSetting(settingName("Lines"), true))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), false))
    private val outlineWidth by setting(this, FloatSetting(settingName("Outline Width"), 1.0f, 0.1f..20.0f, 1.0f, { outline }))
    private val outlineColor by setting(this, ColorSetting(settingName("Outline Color"), ColorRGB(255, 255, 255), { outline }))

    private val dot by setting(this, BooleanSetting(settingName("Dot"), false))
    private val dotRadius by setting(this, FloatSetting(settingName("Dot Radius"), 1.0f, 0.1f..20.0f, 1.0f, { dot }))
    private val dotSeg by setting(this, IntegerSetting(settingName("Dot Segments"), 10, 1..30, 1, { dot }))

    private val dotOutline by setting(this, BooleanSetting(settingName("Dot Outline"), false))
    private val dotOutlineRadius by setting(this, FloatSetting(settingName("Dot Outline Radius"), 1.0f, 0.1f..20.0f, 1.0f, { dotOutline }))
    private val dotOutlineColor by setting(this, ColorSetting(settingName("Dot Outline Color"), ColorRGB(255, 255, 255), { dotOutline }))
    private val dotOutlineWidth by setting(this, FloatSetting(settingName("Dot Outline Width"), 1.0f, 0.1f..20.0f, 1.0f, { dotOutline }))

    private val length by setting(this, FloatSetting(settingName("Length"), 5.5f, 0.5f..50.0f, 1.0f, { lines || outline }))
    private val gapSize by setting(this, FloatSetting(settingName("Gap Size"), 2.0f, 0.5f..20.0f, 1.0f, { lines || outline }))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 1.0f, 0.1f..20.0f, 1.0f, { lines }))

    init {
        listener<Render2DEvent.Absolute> {
            val resolution = ScaledResolution(mc)
            val middleX = resolution.scaledWidth / 2
            val middleY = resolution.scaledHeight / 2

            GlStateUtils.pushMatrixAll()
            try {
                GlStateUtils.blend(true)
                GlStateUtils.depth(false)
                GlStateUtils.texture2d(true)

                val currentGap = if (MovementUtils.isInputting() && gapMode == GapMode.DYNAMIC) gapSize else 0.0f
                val totalGap = gapSize + currentGap

                if (lines) {
                    RenderUtils2D.drawRectFilled(middleX.toFloat() - lineWidth, middleY.toFloat() - (totalGap + length), middleX.toFloat() + lineWidth, middleY.toFloat() - totalGap, color)
                    RenderUtils2D.drawRectFilled(middleX.toFloat() - lineWidth, middleY.toFloat() + totalGap, middleX.toFloat() + lineWidth, middleY.toFloat() + (totalGap + length), color)
                    RenderUtils2D.drawRectFilled(middleX.toFloat() - (totalGap + length), middleY.toFloat() - lineWidth, middleX.toFloat() - totalGap, middleY.toFloat() + lineWidth, color)
                    RenderUtils2D.drawRectFilled(middleX.toFloat() + totalGap, middleY.toFloat() - lineWidth, middleX.toFloat() + (totalGap + length), middleY.toFloat() + lineWidth, color)
                }

                if (outline) {
                    RenderUtils2D.drawRectOutline(middleX.toFloat() - lineWidth, middleY.toFloat() - (totalGap + length), middleX.toFloat() + lineWidth, middleY.toFloat() - totalGap, outlineWidth, outlineColor)
                    RenderUtils2D.drawRectOutline(middleX.toFloat() - lineWidth, middleY.toFloat() + totalGap, middleX.toFloat() + lineWidth, middleY.toFloat() + (totalGap + length), outlineWidth, outlineColor)
                    RenderUtils2D.drawRectOutline(middleX.toFloat() - (totalGap + length), middleY.toFloat() - lineWidth, middleX.toFloat() - totalGap, middleY.toFloat() + lineWidth, outlineWidth, outlineColor)
                    RenderUtils2D.drawRectOutline(middleX.toFloat() + totalGap, middleY.toFloat() - lineWidth, middleX.toFloat() + (totalGap + length), middleY.toFloat() + lineWidth, outlineWidth, outlineColor)
                }

                if (dot) {
                    RenderUtils2D.drawCircleFilled(Vec2f(middleX.toDouble(), middleY.toDouble()), dotRadius, dotSeg, color)
                }

                if (dotOutline) {
                    RenderUtils2D.drawCircleOutline(Vec2f(middleX.toDouble(), middleY.toDouble()), dotOutlineRadius, 150, dotOutlineWidth, dotOutlineColor)
                }
            } finally {
                GlStateUtils.popMatrixAll()
                GlStateUtils.depth(true)
            }
        }
    }

    enum class ColorMode(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"), GRADIENT("Gradient")
    }

    enum class GapMode(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"), DYNAMIC("Dynamic")
    }
}
