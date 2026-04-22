package dev.wizard.meta.util.inventory.operation

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.inventory.*
import dev.wizard.meta.util.inventory.slot.HotbarSlot
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot

fun InventoryTask.Builder.pickUp(windowID: Int = 0, slot: (SafeClientEvent) -> Slot?) {
    this + Click(windowID, slot, 0, ClickType.PICKUP)
}

fun InventoryTask.Builder.pickUp(windowID: Int = 0, slot: Slot) {
    this + Click(windowID, { slot }, 0, ClickType.PICKUP)
}

fun InventoryTask.Builder.pickUpAll(windowID: Int = 0, slot: (SafeClientEvent) -> Slot?) {
    this + Click(windowID, slot, 0, ClickType.PICKUP_ALL)
}

fun InventoryTask.Builder.pickUpAll(windowID: Int = 0, slot: Slot) {
    this + Click(windowID, { slot }, 0, ClickType.PICKUP_ALL)
}

fun InventoryTask.Builder.quickMove(windowID: Int = 0, slot: (SafeClientEvent) -> Slot?) {
    this + Click(windowID, slot, 0, ClickType.QUICK_MOVE)
}

fun InventoryTask.Builder.quickMove(windowID: Int = 0, slot: Slot) {
    this + Click(windowID, { slot }, 0, ClickType.QUICK_MOVE)
}

fun InventoryTask.Builder.swapWith(windowID: Int = 0, slot: (SafeClientEvent) -> Slot?, hotbarSlot: (SafeClientEvent) -> HotbarSlot?) {
    this + Click(windowID, slot, { hotbarSlot(it)?.hotbarSlot }, ClickType.SWAP)
}

fun InventoryTask.Builder.swapWith(windowID: Int = 0, slot: Slot, hotbarSlot: HotbarSlot) {
    this + Click(windowID, slot, hotbarSlot.hotbarSlot, ClickType.SWAP)
}

fun InventoryTask.Builder.throwOne(windowID: Int = 0, slot: Slot) {
    this + Click(windowID, slot, 0, ClickType.THROW)
}

fun InventoryTask.Builder.throwAll(windowID: Int = 0, slot: Slot) {
    this + Click(windowID, slot, 1, ClickType.THROW)
}

fun InventoryTask.Builder.action(block: (SafeClientEvent) -> Unit) {
    this + object : Step {
        override fun run(event: SafeClientEvent): StepFuture {
            block(event)
            return InstantFuture
        }
    }
}
