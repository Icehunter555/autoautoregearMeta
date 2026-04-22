package dev.wizard.meta.util.inventory.slot

import dev.wizard.meta.util.Wrapper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import java.util.function.Predicate

fun EntityPlayer.anyHotbarSlot(predicate: Predicate<ItemStack>? = null): HotbarSlot {
    val hotbarSlots = getHotbarSlots()
    return hotbarSlots.firstEmpty()
        ?: hotbarSlots.firstByStack(predicate)
        ?: getFirstHotbarSlot()
}

val Slot.isHotbarSlot: Boolean
    get() {
        return slotNumber in 36..44 && inventory === Wrapper.player?.inventory
    }

val Slot.hotbarIndex: Int
    get() = if (isHotbarSlot) slotNumber - 36 else -1

fun Slot.toHotbarSlotOrNull(): HotbarSlot? {
    return if (isHotbarSlot) HotbarSlot(this) else null
}
