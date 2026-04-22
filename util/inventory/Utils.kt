package dev.wizard.meta.util.inventory

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.inventory.slot.getAllSlotsPrioritized
import dev.wizard.meta.util.inventory.slot.getSlots
import dev.wizard.meta.util.inventory.slot.getCraftingSlots
import dev.wizard.meta.util.inventory.slot.firstItem
import dev.wizard.meta.util.threads.onMainThreadSafe
import net.minecraft.block.state.IBlockState
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow

fun SafeClientEvent.equipBestTool(blockState: IBlockState) {
    findBestTool(blockState)?.let { swapToSlot(it) }
}

fun SafeClientEvent.findBestTool(blockState: IBlockState): Slot? {
    var maxSpeed = 0.0f
    var bestSlot: Slot? = null
    for (slot in getAllSlotsPrioritized()) {
        val stack = slot.stack
        if (stack.isEmpty) continue
        if (!stack.item.isTool) continue
        var speed = stack.getDestroySpeed(blockState)
        if (speed > 1.0f) {
            val efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack)
            if (efficiency > 0) {
                speed += (efficiency * efficiency).toFloat() + 1.0f
            }
        }
        if (speed > maxSpeed) {
            maxSpeed = speed
            bestSlot = slot
        }
    }
    return bestSlot
}

fun SafeClientEvent.removeHoldingItem() {
    if (player.inventory.itemStack.isEmpty) return
    val container = player.inventoryContainer
    var slot = container.getSlots(9..44).firstItem(Items.AIR)
    if (slot == null) {
        slot = getCraftingSlots().firstItem(Items.AIR)
    }
    val slotId = slot?.slotNumber ?: -999
    clickSlot(0, slotId, 0, ClickType.PICKUP)
}

fun SafeClientEvent.clickSlot(windowID: Int, slot: Slot, mouseButton: Int, type: ClickType): Short {
    return clickSlot(windowID, slot.slotNumber, mouseButton, type)
}

fun SafeClientEvent.clickSlot(windowID: Int, slotId: Int, mouseButton: Int, type: ClickType): Short {
    synchronized(InventoryTaskManager) {
        val container = getContainerForID(windowID) ?: return Short.MIN_VALUE
        val playerInventory = player.inventory
        val transactionID = container.getNextTransactionID(playerInventory)
        val itemStack = container.slotClick(slotId, mouseButton, type, player)
        connection.sendPacket(CPacketClickWindow(windowID, slotId, mouseButton, type, itemStack, transactionID))
        onMainThreadSafe { playerController.updateController() }
        return transactionID
    }
}

private fun SafeClientEvent.getContainerForID(windowID: Int): Container? {
    return if (windowID == 0) {
        player.inventoryContainer
    } else {
        val container = player.openContainer
        if (container.windowId == windowID) container else null
    }
}

fun ItemStack.isStackable(other: ItemStack): Boolean {
    return count < maxStackSize && isCompatible(other)
}

fun ItemStack.isCompatible(other: ItemStack): Boolean {
    return isEmpty || other.isEmpty || (item === other.item && ItemStack.areItemStackTagsEqual(this, other))
}
