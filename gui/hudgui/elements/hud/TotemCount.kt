package dev.wizard.meta.gui.hudgui.elements.hud

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.inventory.slot.allSlots
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object TotemCount : LabelHud("Totem Count", category = Category.HUD, description = "Shows how many totems u have") {

    private val showIcon by setting(this, "Show Icon", true)
    private val totemStack = ItemStack(Items.TOTEM_OF_UNDYING, -1)

    override val hudWidth by FrameFloat { if (showIcon) 20.0f else displayText.width }
    override val hudHeight by FrameFloat { if (showIcon) 20.0f else displayText.height }

    override fun updateText(event: SafeClientEvent) {
        val count = event.player.allSlots.asSequence()
            .filter { it.stack.item == Items.TOTEM_OF_UNDYING }
            .sumOf { it.stack.count }

        if (showIcon) {
            totemStack.count = count + 1
        } else if (count > 0) {
            displayText.add("Totems", ClickGUI.text)
            displayText.addLine("x$count", ClickGUI.primary)
        }
    }

    override fun renderHud() {
        if (showIcon) {
            GlStateManager.pushMatrix()
            RenderUtils2D.drawItem(totemStack, 2, 2, (totemStack.count - 1).toString())
            GlStateManager.popMatrix()
        } else {
            super.renderHud()
        }
    }
}
