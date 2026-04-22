package dev.wizard.meta.gui.hudgui.elements.hud

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.inventory.slot.storageSlots

object InventoryViewer : HudElement("Inventory Viewer", category = Category.HUD, description = "Items in Inventory") {

    private val border by setting(this, "Border", true)
    private val background by setting(this, "Background", true)

    override val hudWidth = 162.0f
    override val hudHeight = 54.0f

    override fun renderHud() {
        super.renderHud()
        SafeClientEvent.instance?.let {
            drawFrame()
            drawItems(it)
        }
    }

    private fun drawFrame() {
        if (background) {
            RenderUtils2D.drawRectFilled(0.0f, 0.0f, 162.0f, 54.0f, ClickGUI.backGround)
        }
        if (border) {
            RenderUtils2D.drawRectOutline(0.0f, 0.0f, 162.0f, 54.0f, 2.0f, ClickGUI.primary)
        }
    }

    private fun drawItems(event: SafeClientEvent) {
        event.player.storageSlots.forEachIndexed { index, slot ->
            val itemStack = slot.stack
            if (itemStack.isEmpty) return@forEachIndexed
            val slotX = index % 9 * 18 + 1
            val slotY = index / 9 * 18 + 1
            RenderUtils2D.drawItem(itemStack, slotX, slotY)
        }
    }
}
