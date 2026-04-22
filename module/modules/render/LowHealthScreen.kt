package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import net.minecraft.client.renderer.GlStateManager

object LowHealthScreen : Module(
    "LowHPScreen",
    category = Category.RENDER,
    description = "Displays a red overlay when player health is low"
) {
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(128, 5, 5, 219)))
    private val minHealth by setting(this, FloatSetting(settingName("Min Health"), 9.0f, 1.0f..20.0f, 1.0f))

    private var dynamicAlpha = 0
    private var targetAlpha = 0

    init {
        listener<Render2DEvent.Mc> {
            val player = mc.player ?: return@listener
            val health = player.health

            targetAlpha = when {
                health > minHealth -> 0
                health > 9.0f -> 18
                health > 8.0f -> 36
                health > 7.0f -> 54
                health > 6.0f -> 72
                health > 5.0f -> 90
                health > 4.0f -> 108
                health > 3.0f -> 126
                health > 2.0f -> 144
                health > 1.0f -> 162
                health > 0.0f -> 180
                else -> 0
            }

            if (targetAlpha > dynamicAlpha) {
                dynamicAlpha += 3
            } else if (targetAlpha < dynamicAlpha) {
                dynamicAlpha -= 3
            }

            if (health <= minHealth) {
                val width = mc.displayWidth.toFloat()
                val height = mc.displayHeight.toFloat()
                val finalAlpha = (dynamicAlpha + 40).coerceIn(0, 255)
                val overlayColor = color.withAlpha(finalAlpha)

                GlStateManager.pushMatrix()
                GlStateUtils.blend(true)
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
                GlStateUtils.depth(false)
                GlStateUtils.texture2d(false)
                GlStateManager.loadIdentity()
                GlStateManager.matrixMode(5889)
                GlStateManager.loadIdentity()
                GlStateManager.ortho(0.0, width.toDouble(), height.toDouble(), 0.0, 1000.0, 3000.0)
                GlStateManager.matrixMode(5888)
                GlStateManager.loadIdentity()
                GlStateManager.translate(0.0f, 0.0f, -2000.0f)

                RenderUtils2D.drawGradientRectHorizontal(0.0f, 0.0f, width, height, ColorRGB(0, 0, 0, 0), overlayColor)

                GlStateUtils.depth(true)
                GlStateUtils.texture2d(true)
                GlStateManager.popMatrix()
            }
        }
    }
}
