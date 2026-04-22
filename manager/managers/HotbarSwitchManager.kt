package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.HotbarUpdateEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.modules.client.AntiCheat
import dev.wizard.meta.util.inventory.slot.getHotbarSlots
import dev.wizard.meta.util.inventory.slot.hotbarIndex
import dev.wizard.meta.util.inventory.slot.isHotbarSlot
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import net.minecraft.network.play.client.CPacketHeldItemChange

object HotbarSwitchManager : Manager() {
    var serverSideHotbar: Int = 0
        private set
    var swapTime: Long = 0L
        private set
    private var serverSizeItemOverride: ItemStack? = null

    val EntityPlayerSP.serverSideItem: ItemStack
        get() = serverSizeItemOverride ?: inventory.mainInventory[serverSideHotbar]

    fun SafeClientEvent.ghostSwitch(slot: Slot, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(slot: Int, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Slot, block: () -> Unit) {
        synchronized(InventoryTaskManager) {
            serverSizeItemOverride = slot.stack
            if (slot.hotbarIndex != serverSideHotbar) {
                override.mode.switch(this, slot, block)
                return
            }
            serverSizeItemOverride = null
        }
        block()
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Int, block: () -> Unit) {
        ghostSwitch(override, player.getHotbarSlots()[slot], block)
    }

    init {
        listener<PacketEvent.Send>(priority = Int.MIN_VALUE) { event ->
            val packet = event.packet
            if (event.cancelled || packet !is CPacketHeldItemChange) return@listener
            
            val prev: Int
            synchronized(InventoryTaskManager) {
                prev = serverSideHotbar
                serverSideHotbar = packet.heldItemHotbarIndex
                swapTime = System.currentTimeMillis()
            }
            if (prev != packet.heldItemHotbarIndex) {
                HotbarUpdateEvent(prev, serverSideHotbar).post()
            }
        }
    }

    enum class BypassMode(val displayName: CharSequence) {
        NONE("NONE") {
            override fun switch(safeClientEvent: SafeClientEvent, targetSlot: Slot, block: () -> Unit) {
                if (!targetSlot.isHotbarSlot) {
                    SWAP.switch(safeClientEvent, targetSlot, block)
                    return
                }
                val prevSlot = serverSideHotbar
                safeClientEvent.connection.sendPacket(CPacketHeldItemChange(targetSlot.hotbarIndex))
                block()
                safeClientEvent.connection.sendPacket(CPacketHeldItemChange(prevSlot))
            }
        },
        MOVE("MOVE") {
            override fun switch(safeClientEvent: SafeClientEvent, targetSlot: Slot, block: () -> Unit) {
                val hotbarSlots = safeClientEvent.player.getHotbarSlots()
                val inventory = safeClientEvent.player.inventory
                val inventoryContainer = safeClientEvent.player.inventoryContainer
                val heldItem = safeClientEvent.player.serverSideItem
                val targetItem = targetSlot.stack
                
                safeClientEvent.connection.sendPacket(CPacketClickWindow(0, targetSlot.slotNumber, 0, ClickType.PICKUP, targetItem, inventoryContainer.getNextTransactionID(inventory)))
                safeClientEvent.connection.sendPacket(CPacketClickWindow(0, hotbarSlots[serverSideHotbar].slotNumber, 0, ClickType.PICKUP, heldItem, inventoryContainer.getNextTransactionID(inventory)))
                block()
                safeClientEvent.connection.sendPacket(CPacketClickWindow(0, hotbarSlots[serverSideHotbar].slotNumber, 0, ClickType.PICKUP, targetItem, inventoryContainer.getNextTransactionID(inventory)))
                safeClientEvent.connection.sendPacket(CPacketClickWindow(0, targetSlot.slotNumber, 0, ClickType.PICKUP, ItemStack.EMPTY, inventoryContainer.getNextTransactionID(inventory)))
            }
        },
        SWAP("SWAP") {
            override fun switch(safeClientEvent: SafeClientEvent, targetSlot: Slot, block: () -> Unit) {
                val transactionID = safeClientEvent.player.inventoryContainer.getNextTransactionID(safeClientEvent.player.inventory)
                safeClientEvent.connection.sendPacket(CPacketClickWindow(0, targetSlot.slotNumber, serverSideHotbar, ClickType.SWAP, ItemStack.EMPTY, transactionID))
                block()
                safeClientEvent.connection.sendPacket(CPacketClickWindow(0, targetSlot.slotNumber, serverSideHotbar, ClickType.SWAP, ItemStack.EMPTY, transactionID))
            }
        },
        PICK("PICK") {
            override fun switch(safeClientEvent: SafeClientEvent, targetSlot: Slot, block: () -> Unit) {
                if (targetSlot.slotNumber == 45 || targetSlot.slotNumber < 9) {
                    SWAP.switch(safeClientEvent, targetSlot, block)
                    return
                }
                val number = targetSlot.slotNumber % 36
                safeClientEvent.playerController.pickItem(number)
                block()
                safeClientEvent.playerController.pickItem(number)
            }
        };

        abstract fun switch(safeClientEvent: SafeClientEvent, targetSlot: Slot, block: () -> Unit)
    }

    enum class Override(val displayName: CharSequence) {
        DEFAULT("DEFAULT") {
            override val mode: BypassMode get() = AntiCheat.ghostSwitchBypass
        },
        NONE("NONE") {
            override val mode: BypassMode = BypassMode.NONE
        },
        MOVE("MOVE") {
            override val mode: BypassMode = BypassMode.MOVE
        },
        SWAP("SWAP") {
            override val mode: BypassMode = BypassMode.SWAP
        },
        PICK("PICK") {
            override val mode: BypassMode = BypassMode.PICK
        };

        abstract val mode: BypassMode
    }
}
