package dev.wizard.meta.gui.hudgui.elements.hud

import dev.wizard.meta.event.*
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.VAlign
import dev.wizard.meta.graphics.color.ColorGradient
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.LambdaUtilsKt.atTrue
import dev.wizard.meta.util.inventory.slot.armorSlots
import dev.wizard.meta.util.inventory.slot.countItem
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import kotlin.math.max

object ClassicArmor : HudElement("ClassicArmor", category = Category.HUD, description = "show armor in classic horizontal layout") {

    private val armorCount by setting(this, "ArmorCount", true)
    private val countElytras by setting(this, "CountElytras", false, visibility = { armorCount })
    private val durabilityPercentage by setting(this, "Durability Percentage", true)
    private val countBar by setting(this, "Count Bar", true, visibility = { armorCount })
    private val countColor by setting(this, "Count Color", false)
    private val customCountColors by setting(this, "Custom Count Colors", false)
    private val customCountLow by setting(this, "Low Count Color", ColorRGB(200, 20, 20), visibility = atTrue(::customCountColors))
    private val customCountMedium by setting(this, "Medium Count Color", ColorRGB(240, 220, 20), visibility = atTrue(::customCountColors))
    private val customCountHigh by setting(this, "High Count Color", ColorRGB(20, 232, 20), visibility = atTrue(::customCountColors))

    private const val hudWidth = 80.0f
    private var stringHeight = 20.0f
    private var renderStringHeight = 20.0f
    private val armorCounts = IntArray(4)

    private val duraColorGradient = ColorGradient(
        ColorGradient.Stop(0.0f, ColorRGB(200, 20, 20)),
        ColorGradient.Stop(50.0f, ColorRGB(240, 220, 20)),
        ColorGradient.Stop(100.0f, ColorRGB(20, 232, 20))
    )

    init {
        parallelListener<TickEvent.Post> {
            val slots = player.armorSlots
            armorCounts[0] = slots.countItem(Items.DIAMOND_HELMET)
            val chestItem = if (countElytras && player.inventory.getStackInSlot(38).item == Items.ELYTRA) Items.ELYTRA else Items.DIAMOND_CHESTPLATE
            armorCounts[1] = slots.countItem(chestItem)
            armorCounts[2] = slots.countItem(Items.DIAMOND_LEGGINGS)
            armorCounts[3] = slots.countItem(Items.DIAMOND_BOOTS)
        }
    }

    override val hudWidth get() = ClassicArmor.hudWidth
    override val hudHeight get() = renderStringHeight

    override fun renderHud() {
        super.renderHud()
        stringHeight = 0.0f
        runSafeSuspend {
            GlStateManager.pushMatrix()
            player.armorInventoryList.reversed().forEachIndexed { index, itemStack ->
                drawItem(index, itemStack)
            }
            GlStateManager.popMatrix()
        }
        renderStringHeight = if (durabilityPercentage) stringHeight + 24.0f else 20.0f
    }

    private fun drawItem(index: Int, itemStack: ItemStack) {
        if (itemStack.isEmpty) {
            GlStateManager.translate(20.0f, 0.0f, 0.0f)
            return
        }

        val itemX = 2
        val itemY = if (dockingV != VAlign.TOP) (MainFontRenderer.height + 4.0f).toInt() else 2
        RenderUtils2D.drawItem(itemStack, itemX, itemY)
        drawDura(itemStack, itemX, itemY)

        if (armorCount) {
            drawCountBar(index, itemX, itemY)
            val string = armorCounts[index].toString()
            val width = MainFontRenderer.getWidth(string)
            val height = MainFontRenderer.height
            val color = if (countColor) getCountGradient().get(armorCounts[index].toFloat()) else ColorRGB(255, 255, 255).unbox()
            MainFontRenderer.drawString(string, itemX + 16.0f - width, itemY + 16.0f - height, color, 0.0f, false)
        }
        GlStateManager.translate(20.0f, 0.0f, 0.0f)
    }

    private fun drawDura(itemStack: ItemStack, x: Int, y: Int) {
        if (!itemStack.isItemDamaged) return
        val dura = itemStack.maxDamage - itemStack.itemDamage
        val duraMultiplier = dura.toFloat() / itemStack.maxDamage.toFloat()
        val duraPercent = MathUtils.round(duraMultiplier * 100.0f, 1)
        val color = duraColorGradient.get(duraPercent)

        if (durabilityPercentage) {
            val string = "$dura/${itemStack.maxDamage}  ($duraPercent%)"
            val height = MainFontRenderer.height
            stringHeight = max(height, stringHeight)
            val duraX = 10.0f - MainFontRenderer.getWidth(string) * 0.5f
            val duraY = if (dockingV != VAlign.TOP) 2.0f else 22.0f
            MainFontRenderer.drawString(string, duraX, duraY, color, 0.0f, false)
        }
    }

    private fun drawCountBar(index: Int, x: Int, y: Int) {
        if (!countBar) return
        val count = armorCounts[index]
        val maxCount = 127.0f
        val countMultiplier = (count.toFloat() / maxCount).coerceIn(0.0f, 1.0f)
        val countPercent = countMultiplier * 100.0f
        val color = getCountGradient().get(countPercent)
        val countBarWidth = max(16.0f * countMultiplier, 0.0f)

        RenderUtils2D.drawRectFilled(x.toFloat(), y + 16.0f, x + 16.0f, y + 18.0f, ColorRGB(0, 0, 0))
        RenderUtils2D.drawRectFilled(x.toFloat(), y + 16.0f, x + countBarWidth, y + 18.0f, color)
    }

    private fun getCountGradient(): ColorGradient {
        return if (customCountColors) {
            ColorGradient(
                ColorGradient.Stop(0.0f, customCountLow),
                ColorGradient.Stop(50.0f, customCountMedium),
                ColorGradient.Stop(100.0f, customCountHigh)
            )
        } else {
            ColorGradient(
                ColorGradient.Stop(0.0f, ColorRGB(200, 20, 20)),
                ColorGradient.Stop(50.0f, ColorRGB(240, 220, 20)),
                ColorGradient.Stop(100.0f, ColorRGB(20, 232, 20))
            )
        }
    }
}
