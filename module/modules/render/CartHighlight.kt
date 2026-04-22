package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorGradient
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityMinecartTNT
import net.minecraft.util.math.BlockPos

object CartHighlight : Module(
    "CartHighlight",
    category = Category.RENDER,
    description = "count for cart"
) {
    private val range by setting(this, IntegerSetting(settingName("Range"), 10, 1..50, 5))
    private val mode by setting(this, EnumSetting(settingName("Mode"), Mode.COUNT))
    private val cartColor1 by setting(this, ColorSetting(settingName("Cart Color 1"), ColorRGB(255, 255, 0)))
    private val cartColor2 by setting(this, ColorSetting(settingName("Cart Color 2"), ColorRGB(255, 153, 51)))
    private val cartColor3 by setting(this, ColorSetting(settingName("Cart Color 3"), ColorRGB(255, 0, 0)))

    private val cartColor: ColorGradient
        get() = ColorGradient(
            ColorGradient.Stop(10.0f, cartColor1.rgba),
            ColorGradient.Stop(16.0f, cartColor2.rgba),
            ColorGradient.Stop(21.0f, cartColor3.rgba)
        )

    fun getCartColor(count: Float): ColorRGB {
        return ColorRGB(cartColor.get(count)).withAlpha(255)
    }

    init {
        safeListener<Render2DEvent.Absolute> {
            val cartMap = LinkedHashMap<BlockPos, MutableList<EntityMinecartTNT>>()
            for (cart in world.loadedEntityList) {
                if (cart is EntityMinecartTNT && player.getDistance(cart) <= range) {
                    val railPos = BlockPos(cart.posX, cart.posY, cart.posZ)
                    cartMap.getOrPut(railPos) { ArrayList() }.add(cart)
                }
            }

            for ((_, carts) in cartMap) {
                val count = carts.size
                val firstCart = carts.firstOrNull() ?: continue
                val text = if (mode == Mode.COUNT) count.toString() else "!"

                val center = firstCart.positionVector.add(0.0, 0.25, 0.0)
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                val scale = (9.0f / Math.pow(1.5, distFactor).toFloat()).coerceAtLeast(1.0f)

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f

                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, getCartColor(count.toFloat()), scale)
            }
        }
    }

    private enum class Mode { SYMBOL, COUNT }
}
