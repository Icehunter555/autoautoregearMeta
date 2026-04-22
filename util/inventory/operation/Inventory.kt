package dev.wizard.meta.util.inventory.operation

import dev.wizard.meta.util.inventory.InventoryTask
import net.minecraft.inventory.Slot

fun InventoryTask.Builder.moveTo(slotFrom: Slot, slotTo: Slot) {
    pickUp(slotFrom)
    pickUp(slotTo)
    pickUp(slotFrom)
}

fun InventoryTask.Builder.moveTo(windowID: Int, slotFrom: Slot, slotTo: Slot) {
    pickUp(windowID, slotFrom)
    pickUp(windowID, slotTo)
    pickUp(windowID, slotFrom)
}

fun InventoryTask.Builder.moveAllTo(slotTo: Slot) {
    pickUp(slotTo)
    pickUpAll(slotTo)
    pickUp(slotTo)
}

fun InventoryTask.Builder.moveAllTo(windowID: Int, slotTo: Slot) {
    pickUp(windowID, slotTo)
    pickUpAll(windowID, slotTo)
    pickUp(windowID, slotTo)
}
