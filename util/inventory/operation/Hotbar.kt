package dev.wizard.meta.util.inventory.operation

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.util.accessor.syncCurrentPlayItem
import dev.wizard.meta.util.inventory.inventoryTaskNow
import dev.wizard.meta.util.inventory.slot.*
import net.minecraft.block.Block
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import java.util.function.Predicate

fun SafeClientEvent.swapToBlockOrMove(block: Block, predicateItem: Predicate<ItemStack>? = null, predicateSlot: Predicate<ItemStack>? = null): Boolean {
    return if (swapToBlock(block, predicateItem)) {
        true
    } else {
        val storageSlot = getStorageSlots().firstBlock(block, predicateItem)
        if (storageSlot != null) {
            val slotTo = player.anyHotbarSlot(predicateSlot)
            inventoryTaskNow {
                swapWith(storageSlot, slotTo)
            }
            true
        } else {
            false
        }
    }
}

inline fun <reified I : Item> SafeClientEvent.swapToItemOrMove(noinline predicateItem: Predicate<ItemStack>? = null, noinline predicateSlot: Predicate<ItemStack>? = null): Boolean {
    val hotbarSlot = getHotbarSlots().firstByStack { it.item is I && (predicateItem == null || predicateItem.test(it)) }
    return if (hotbarSlot != null) {
        swapToSlot(hotbarSlot)
        true
    } else {
        val storageSlot = getStorageSlots().firstByStack { it.item is I && (predicateItem == null || predicateItem.test(it)) }
        if (storageSlot != null) {
            val slotTo = player.anyHotbarSlot(predicateSlot)
            inventoryTaskNow {
                swapWith(storageSlot, slotTo)
            }
            true
        } else {
            false
        }
    }
}

fun SafeClientEvent.swapToItemOrMove(item: Item, predicateItem: Predicate<ItemStack>? = null, predicateSlot: Predicate<ItemStack>? = null): Boolean {
    return if (swapToItem(item, predicateItem)) {
        true
    } else {
        val storageSlot = getStorageSlots().firstItem(item, predicateItem)
        if (storageSlot != null) {
            val slotTo = player.anyHotbarSlot(predicateSlot)
            inventoryTaskNow {
                swapWith(storageSlot, slotTo)
            }
            true
        } else {
            false
        }
    }
}

inline fun <reified B : Block> SafeClientEvent.swapToBlock(noinline predicate: Predicate<ItemStack>? = null): Boolean {
    val hotbarSlot = getHotbarSlots().firstByStack {
        val item = it.item
        item is ItemBlock && item.block is B && (predicate == null || predicate.test(it))
    }
    return if (hotbarSlot != null) {
        swapToSlot(hotbarSlot)
        true
    } else {
        false
    }
}

fun SafeClientEvent.swapToBlock(block: Block, predicate: Predicate<ItemStack>? = null): Boolean {
    val hotbarSlot = getHotbarSlots().firstBlock(block, predicate)
    return if (hotbarSlot != null) {
        swapToSlot(hotbarSlot)
        true
    } else {
        false
    }
}

inline fun <reified I : Item> SafeClientEvent.swapToItem(noinline predicate: Predicate<ItemStack>? = null): Boolean {
    val hotbarSlot = getHotbarSlots().firstByStack { it.item is I && (predicate == null || predicate.test(it)) }
    return if (hotbarSlot != null) {
        swapToSlot(hotbarSlot)
        true
    } else {
        false
    }
}

fun SafeClientEvent.swapToItem(item: Item, predicate: Predicate<ItemStack>? = null): Boolean {
    val hotbarSlot = getHotbarSlots().firstItem(item, predicate)
    return if (hotbarSlot != null) {
        swapToSlot(hotbarSlot)
        true
    } else {
        false
    }
}

fun SafeClientEvent.swapToSlot(slot: Slot) {
    if (slot.isHotbarSlot) {
        swapToSlot(slot.hotbarIndex)
    } else {
        val slotTo = player.anyHotbarSlot()
        inventoryTaskNow {
            swapWith(slot, slotTo)
        }
    }
}

fun SafeClientEvent.swapToSlot(slot: Int) {
    if (slot !in 0..8) return
    synchronized(InventoryTaskManager) {
        player.inventory.currentItem = slot
        playerController.syncCurrentPlayItem()
    }
}
