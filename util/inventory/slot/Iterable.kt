package dev.wizard.meta.util.inventory.slot

import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.util.inventory.id
import dev.wizard.meta.util.inventory.isTool
import net.minecraft.block.Block
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import java.util.function.Predicate

fun Iterable<Slot>.hasItem(item: Item, predicate: Predicate<ItemStack> = Predicate { true }): Boolean {
    return any { it.stack.item === item && predicate.test(it.stack) }
}

inline fun <reified I : Item> Iterable<Slot>.hasItem(): Boolean {
    return any { it.stack.item is I }
}

fun Iterable<Slot>.hasAnyItem(): Boolean {
    return any { !it.stack.isEmpty }
}

fun Iterable<Slot>.hasEmpty(): Boolean {
    return any { it.stack.isEmpty }
}

fun Iterable<Slot>.countEmpty(): Int {
    return count { it.stack.isEmpty }
}

inline fun <reified B : Block> Iterable<Slot>.countBlock(noinline predicate: Predicate<ItemStack>? = null): Int {
    return countByStack {
        val item = it.item
        item is ItemBlock && item.block is B && (predicate == null || predicate.test(it))
    }
}

fun Iterable<Slot>.countBlock(block: Block, predicate: Predicate<ItemStack>? = null): Int {
    return countByStack {
        val item = it.item
        item is ItemBlock && item.block === block && (predicate == null || predicate.test(it))
    }
}

inline fun <reified I : Item> Iterable<Slot>.countItem(noinline predicate: Predicate<ItemStack>? = null): Int {
    return countByStack { it.item is I && (predicate == null || predicate.test(it)) }
}

fun Iterable<Slot>.countItem(item: Item, predicate: Predicate<ItemStack>? = null): Int {
    return countByStack { it.item === item && (predicate == null || predicate.test(it)) }
}

fun Iterable<Slot>.countID(itemID: Int, predicate: Predicate<ItemStack>? = null): Int {
    return countByStack { it.item.id == itemID && (predicate == null || predicate.test(it)) }
}

fun Iterable<Slot>.countByStack(predicate: (ItemStack) -> Boolean = { true }): Int {
    var n = 0
    for (slot in this) {
        val stack = slot.stack
        if (predicate(stack)) {
            n += stack.count
        }
    }
    return n
}

fun Iterable<Slot>.countByStack(predicate: Predicate<ItemStack>? = null): Int {
    var n = 0
    for (slot in this) {
        val stack = slot.stack
        if (predicate == null || predicate.test(stack)) {
            n += stack.count
        }
    }
    return n
}

fun <T : Slot> Iterable<T>.firstEmpty(): T? {
    return firstByStack { it.isEmpty }
}

inline fun <reified B : Block> Iterable<HotbarSlot>.firstBlock(noinline predicate: Predicate<ItemStack>? = null): HotbarSlot? {
    return firstByStack {
        val item = it.item
        item is ItemBlock && item.block is B && (predicate == null || predicate.test(it))
    }
}

inline fun <reified B : Block> Iterable<Slot>.firstBlock(noinline predicate: Predicate<ItemStack>? = null): Slot? {
    return firstByStack {
        val item = it.item
        item is ItemBlock && item.block is B && (predicate == null || predicate.test(it))
    }
}

fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null): T? {
    return firstByStack {
        val item = it.item
        item is ItemBlock && item.block === block && (predicate == null || predicate.test(it))
    }
}

inline fun <reified I : Item, T : Slot> Iterable<T>.firstItem(noinline predicate: Predicate<ItemStack>? = null): T? {
    return firstByStack { it.item is I && (predicate == null || predicate.test(it)) }
}

fun <T : Slot> Iterable<T>.firstItem(item: Item, predicate: Predicate<ItemStack>? = null): T? {
    return firstByStack { it.item === item && (predicate == null || predicate.test(it)) }
}

fun <T : Slot> Iterable<T>.firstID(itemID: Int, predicate: Predicate<ItemStack>? = null): T? {
    return firstByStack { it.item.id == itemID && (predicate == null || predicate.test(it)) }
}

fun <T : Slot> Iterable<T>.firstByStack(predicate: (ItemStack) -> Boolean = { true }): T? {
    for (slot in this) {
        if (predicate(slot.stack)) return slot
    }
    return null
}

fun <T : Slot> Iterable<T>.firstByStack(predicate: Predicate<ItemStack>? = null): T? {
    for (slot in this) {
        if (predicate == null || predicate.test(slot.stack)) return slot
    }
    return null
}

inline fun <reified B : Block, T : Slot> Iterable<T>.filterByBlock(noinline predicate: Predicate<ItemStack>? = null): List<T> {
    return filterByStack {
        val item = it.item
        item is ItemBlock && item.block is B && (predicate == null || predicate.test(it))
    }
}

fun <T : Slot> Iterable<T>.filterByBlock(block: Block, predicate: Predicate<ItemStack>? = null): List<T> {
    return filterByStack {
        val item = it.item
        item is ItemBlock && item.block === block && (predicate == null || predicate.test(it))
    }
}

inline fun <reified I : Item, T : Slot> Iterable<T>.filterByItem(noinline predicate: Predicate<ItemStack>? = null): List<T> {
    return filterByStack { it.item is I && (predicate == null || predicate.test(it)) }
}

fun <T : Slot> Iterable<T>.filterByItem(item: Item, predicate: Predicate<ItemStack>? = null): List<T> {
    return filterByStack { it.item === item && (predicate == null || predicate.test(it)) }
}

fun <T : Slot> Iterable<T>.filterByID(itemID: Int, predicate: Predicate<ItemStack>? = null): List<T> {
    return filterByStack { it.item.id == itemID && (predicate == null || predicate.test(it)) }
}

fun <T : Slot> Iterable<T>.filterByStack(predicate: Predicate<ItemStack>? = null): List<T> {
    val result = mutableListOf<T>()
    for (slot in this) {
        if (predicate == null || predicate.test(slot.stack)) {
            result.add(slot)
        }
    }
    return result
}

fun Iterable<Slot>.getCompatibleStack(slotTo: Slot): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null
    
    var maxSlot: Slot? = null
    var maxSize = 0
    
    for (slot in this) {
        if (slot.slotNumber == slotTo.slotNumber) continue
        val stackFrom = slot.stack
        if (!ItemStack.isSame(stackTo, stackFrom) || !ItemStack.areItemStackTagsEqual(stackTo, stackFrom)) continue
        val size = stackFrom.count
        if (size == neededSize) return slot
        if (size == stackFrom.maxStackSize) return slot
        if (size > maxSize) {
            maxSlot = slot
            maxSize = size
        }
    }
    return maxSlot
}

fun Iterable<Slot>.findMaxCompatibleStack(slotTo: Slot, targetItem: Kit.ItemEntry, stack: Boolean): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else 64 - stackTo.count
    if (neededSize <= 0) return null
    
    var maxSlot: Slot? = null
    var maxSize = 0
    
    for (slot in this) {
        if (slot.slotNumber == slotTo.slotNumber) continue
        val stackFrom = slot.stack
        if (!targetItem.equals(stackFrom)) continue
        val size = stackFrom.count
        if (!isEmpty && targetItem.equals(stackFrom) && (!ItemStack.isSame(stackTo, stackFrom) || !ItemStack.areItemStackTagsEqual(stackTo, stackFrom))) continue
        if (size == stackFrom.maxStackSize) return slot
        if (size > maxSize) {
            maxSlot = slot
            maxSize = size
        }
    }
    return maxSlot ?: findMaxCompatibleStack(slotTo, targetItem.item, stack)
}

fun Iterable<Slot>.findMaxCompatibleStack(slotTo: Slot, targetItem: Item, stack: Boolean): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else 64 - stackTo.count
    if (neededSize <= 0) return null
    
    var maxSlot: Slot? = null
    var maxSize = 0
    
    for (slot in this) {
        if (slot.slotNumber == slotTo.slotNumber) continue
        val stackFrom = slot.stack
        if (stackFrom.item !== targetItem) continue
        val size = stackFrom.count
        if (!isEmpty && stackTo.item === targetItem && (!ItemStack.isSame(stackTo, stackFrom) || !ItemStack.areItemStackTagsEqual(stackTo, stackFrom))) continue
        if (size == stackFrom.maxStackSize) return slot
        if (size > maxSize) {
            maxSlot = slot
            maxSize = size
        }
    }
    return maxSlot
}

fun Iterable<Slot>.findFirstCompatibleStack(slotTo: Slot): Slot? {
    return findFirstCompatibleStack(slotTo, slotTo.stack.item)
}

fun Iterable<Slot>.findFirstCompatibleStack(slotTo: Slot, targetItem: Kit.ItemEntry): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null
    
    for (slot in this) {
        if (slot.slotNumber == slotTo.slotNumber) continue
        val stackFrom = slot.stack
        if (!targetItem.equals(stackFrom)) continue
        if (!isEmpty && targetItem.equals(stackFrom) && (!ItemStack.isSame(stackTo, stackFrom) || !ItemStack.areItemStackTagsEqual(stackTo, stackFrom))) continue
        return slot
    }
    return findFirstCompatibleStack(slotTo, targetItem.item)
}

fun Iterable<Slot>.findFirstCompatibleStack(slotTo: Slot, targetItem: Item): Slot? {
    val stackTo = slotTo.stack
    val isEmpty = stackTo.isEmpty
    val neededSize = if (isEmpty) 64 else stackTo.maxStackSize - stackTo.count
    if (neededSize <= 0) return null
    
    for (slot in this) {
        if (slot.slotNumber == slotTo.slotNumber) continue
        val stackFrom = slot.stack
        if (stackFrom.item !== targetItem) continue
        if (!isEmpty && stackTo.item === targetItem && (!ItemStack.isSame(stackTo, stackFrom) || !ItemStack.areItemStackTagsEqual(stackTo, stackFrom))) continue
        return slot
    }
    return null
}
