package dev.wizard.meta.module.modules.render

import dev.fastmc.common.ceilToInt
import dev.wizard.meta.event.events.render.RenderOverlayEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.inventory.getFoodValue
import dev.wizard.meta.util.inventory.getSaturation
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemFood
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.GuiIngameForge
import net.minecraftforge.client.event.RenderGameOverlayEvent
import org.lwjgl.opengl.GL11

object HungerOverlay : Module(
    "HungerOverlay",
    category = Category.RENDER,
    description = "Displays a helpful overlay over your hunger bar."
) {
    private val saturationOverlay by setting(this, BooleanSetting(settingName("Saturation Overlay"), true))
    private val foodHungerOverlay by setting(this, BooleanSetting(settingName("Food Hunger Overlay"), true))
    private val foodSaturationOverlay by setting(this, BooleanSetting(settingName("Food Saturation Overlay"), true))

    private val icons = ResourceLocation("textures/hungeroverlay.png")

    init {
        listener<RenderOverlayEvent.Post> {
            if (it.type != RenderGameOverlayEvent.ElementType.FOOD) return@listener

            val time = (System.currentTimeMillis() % 5000L) / 2500.0f
            val flashAlpha = -0.5f * Math.cos(time.toDouble() * Math.PI).toFloat() + 0.5f

            val player = mc.player ?: return@listener
            val stats = player.foodStats
            val resolution = ScaledResolution(mc)
            val left = resolution.scaledWidth / 2 + 82
            val top = resolution.scaledHeight - GuiIngameForge.right_height + 10

            val stack = player.heldItemMainhand
            val itemFood = stack.item as? ItemFood
            val foodValue = itemFood?.getFoodValue(stack) ?: 0
            val saturation = itemFood?.getSaturation(stack) ?: 0.0f

            val newHungerValue = (stats.foodLevel + foodValue).coerceAtMost(20)
            val newSaturationValue = (stats.saturationLevel + saturation).coerceAtMost(newHungerValue.toFloat())

            GlStateUtils.blend(true)

            if (foodHungerOverlay && foodValue > 0) {
                drawHungerBar(stats.foodLevel, newHungerValue, left, top, flashAlpha)
            }
            if (saturationOverlay) {
                drawSaturationBar(0.0f, stats.saturationLevel, left, top, 1.0f)
            }
            if (foodSaturationOverlay && saturation > 0.0f) {
                drawSaturationBar(Math.floor(stats.saturationLevel.toDouble()).toFloat(), newSaturationValue, left, top, flashAlpha)
            }

            mc.textureManager.bindTexture(Gui.ICONS)
        }
    }

    private fun drawHungerBar(start: Int, end: Int, left: Int, top: Int, alpha: Float) {
        val textureX = if (mc.player?.isPotionActive(MobEffects.HUNGER) == true) 88 else 52
        mc.textureManager.bindTexture(Gui.ICONS)
        drawBarHalf((start / 2.0f).toInt(), end / 2.0f, left, top, textureX, alpha)
    }

    private fun drawSaturationBar(start: Float, end: Float, left: Int, top: Int, alpha: Float) {
        mc.textureManager.bindTexture(icons)
        drawBarFourth((start / 2.0f).toInt(), end / 2.0f, left, top, alpha)
    }

    private fun drawBarHalf(start: Int, end: Float, left: Int, top: Int, textureX: Int, alpha: Float) {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha)
        for (currentBar in start..end.ceilToInt()) {
            val remainBars = (Math.floor((end - currentBar).toDouble() * 2.0).toFloat() / 2.0f).coerceAtMost(1.0f)
            val posX = left - currentBar * 8
            if (remainBars == 1.0f) {
                mc.ingameGUI.drawTexturedModalRect(posX, top, textureX, 27, 9, 9)
            } else if (remainBars == 0.5f) {
                mc.ingameGUI.drawTexturedModalRect(posX, top, textureX + 9, 27, 9, 9)
            }
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
    }

    private fun drawBarFourth(start: Int, end: Float, left: Int, top: Int, alpha: Float) {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha)
        for (currentBar in start..end.ceilToInt()) {
            val remainBars = (Math.floor((end - currentBar).toDouble() * 4.0).toFloat() / 4.0f).coerceAtMost(1.0f)
            val posX = left - currentBar * 8
            if (remainBars == 1.0f) {
                mc.ingameGUI.drawTexturedModalRect(posX, top, 27, 0, 9, 9)
            } else if (remainBars == 0.75f) {
                mc.ingameGUI.drawTexturedModalRect(posX, top, 18, 0, 9, 9)
            } else if (remainBars == 0.5f) {
                mc.ingameGUI.drawTexturedModalRect(posX, top, 9, 0, 9, 9)
            } else if (remainBars == 0.25f) {
                mc.ingameGUI.drawTexturedModalRect(posX, top, 0, 0, 9, 9)
            }
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
    }
}
