package dev.wizard.meta.util.inventory.slot

import dev.wizard.meta.manager.managers.HotbarSwitchManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.Slot

fun EntityPlayer.getAllSlots(): List<Slot> {
    return inventoryContainer.getSlots(1..45)
}

fun EntityPlayer.getAllSlotsPrioritized(): List<Slot> {
    val prioritized = mutableListOf<Slot>()
    prioritized.add(getOffhandSlot())
    val hotbarSlots = getHotbarSlots()
    val current = HotbarSwitchManager.serverSideHotbar
    prioritized.add(hotbarSlots[current])
    for (i in hotbarSlots.indices) {
        if (i != current) {
            prioritized.add(hotbarSlots[i])
        }
    }
    prioritized.addAll(inventoryContainer.getSlots(1..35))
    return prioritized
}

fun EntityPlayer.getArmorSlots(): List<Slot> {
    return inventoryContainer.getSlots(5..8)
}

fun EntityPlayer.getHeadSlot(): Slot = inventoryContainer.inventorySlots[5]
fun EntityPlayer.getChestSlot(): Slot = inventoryContainer.inventorySlots[6]
fun EntityPlayer.getLegsSlot(): Slot = inventoryContainer.inventorySlots[7]
fun EntityPlayer.getFeetSlot(): Slot = inventoryContainer.inventorySlots[8]
fun EntityPlayer.getOffhandSlot(): Slot = inventoryContainer.inventorySlots[45]

fun EntityPlayer.getCraftingSlots(): List<Slot> {
    return inventoryContainer.getSlots(1..4)
}

fun EntityPlayer.getInventorySlots(): List<Slot> {
    return inventoryContainer.getSlots(9..44)
}

fun EntityPlayer.getStorageSlots(): List<Slot> {
    return inventoryContainer.getSlots(9..35)
}

fun EntityPlayer.getHotbarSlots(): List<HotbarSlot> {
    return (36..44).map { HotbarSlot(inventoryContainer.inventorySlots[it]) }
}

fun EntityPlayer.getCurrentHotbarSlot(): HotbarSlot {
    return HotbarSlot(inventoryContainer.getSlot(inventory.currentItem + 36))
}

fun EntityPlayer.getFirstHotbarSlot(): HotbarSlot {
    return HotbarSlot(inventoryContainer.getSlot(36))
}

fun EntityPlayer.getHotbarSlot(slot: Int): HotbarSlot {
    require(slot in 0..8) { "Invalid hotbar slot: $slot" }
    return HotbarSlot(inventoryContainer.inventorySlots[slot + 36])
}

fun Container.getContainerSlots(): List<Slot> {
    return getSlots(0 until getContainerSlotSize())
}

fun Container.getPlayerSlots(): List<Slot> {
    val size = getContainerSlotSize()
    return getSlots(size until size + 36)
}

fun Container.getSlots(range: IntRange): List<Slot> {
    return inventorySlots.subList(range.first, range.last + 1)
}

fun Container.getContainerSlotSize(): Int {
    return if (this is ContainerPlayer) 0 else inventorySlots.size - 36
}
