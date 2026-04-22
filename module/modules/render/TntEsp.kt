package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.util.world.getSelectedBox
import net.minecraft.entity.item.EntityTNTPrimed

object TntEsp : Module(
    "Tnt Esp",
    category = Category.RENDER,
    description = "Highlights TNT blocks and shows their explosion countdown"
) {
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(127, 0, 0)))
    private val aFilled by setting(this, IntegerSetting(settingName("Filled Alpha"), 30, 0..255, 1))

    private val renderer = ESPRenderer()

    init {
        safeListener<Render2DEvent.Absolute> {
            for (entity in world.loadedEntityList) {
                if (entity !is EntityTNTPrimed) continue

                val timeLeft = (entity.fuse - entity.ticksExisted).toFloat() + 1.0f
                val secondsLeft = (timeLeft / 20.0f).coerceAtLeast(0.0f)
                val text = "%.1fs".format(secondsLeft)

                val box = world.getSelectedBox(entity.position)
                val center = box.center
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                val scale = (6.0f / Math.pow(2.0, distFactor).toFloat()).coerceAtLeast(1.0f)

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f
                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, ColorRGB(255, 255, 255), scale)
            }
        }

        safeListener<Render3DEvent> {
            renderer.setAOutline(0)
            renderer.setAFilled(aFilled)
            for (entity in world.loadedEntityList) {
                if (entity !is EntityTNTPrimed) continue
                val box = world.getSelectedBox(entity.position)
                renderer.add(box, color)
            }
            renderer.render(true)
        }
    }
}
