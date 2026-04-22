package dev.wizard.meta.module.modules.player

import dev.fastmc.common.collection.DynamicBitSet
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.Kit
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.inventoryTask
import dev.wizard.meta.util.inventory.operation.pickUp
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import java.util.*

object InventorySorter : Module(
    "InventorySort",
    category = Category.PLAYER,
    description = "Sort out items in inventory",
    modulePriority = 20
) {
    private val clickDelay by setting(this, IntegerSetting(settingName("Click Delay"), 10, 0..1000, 1))
    private val postDelay by setting(this, IntegerSetting(settingName("Post Delay"), 50, 0..1000, 1))

    private val checkSet = DynamicBitSet()
    private var itemArray: Array<Kit.ItemEntry>? = null
    private var lastTask: InventoryTask? = null
    private var lastIndex = 35

    override fun getHudInfo(): String = Kit.getKitName()

    init {
        onEnable {
            val kitArray = Kit.getKitItemArray()
            if (kitArray == null) {
                NoSpamMessage.sendError("${getChatName()} No kit named ${Kit.getKitName()} was not found!")
                disable()
            } else {
                itemArray = kitArray
            }
        }

        onDisable {
            itemArray = null
            lastTask?.cancel()
            lastTask = null
            lastIndex = 35
            checkSet.clear()
        }

        safeConcurrentListener<RunGameLoopEvent.Tick> {
            val array = itemArray ?: run { disable(); return@safeConcurrentListener }
            if (lastTask?.isExecuted == false) return@safeConcurrentListener

            if (lastIndex == 0) {
                NoSpamMessage.sendMessage("${getChatName()} Finished sorting!")
                disable()
                return@safeConcurrentListener
            }

            runSorting(this, array)
        }
    }

    private fun runSorting(event: SafeClientEvent, itemArray: Array<Kit.ItemEntry>) {
        val slots = ArrayList<Slot>()
        DefinedKt.getInventorySlots(event.player).forEach {
            if (!checkSet.contains(it.slotIndex - 9)) slots.add(it)
        }

        for (index in 35 downTo 0) {
            lastIndex = index
            if (checkSet.contains(index)) continue

            val targetItem = itemArray[index]
            if (targetItem.item == Items.AIR) continue

            val slotTo = slots[index]
            val stackTo = slotTo.stack

            if (!targetItem.equals(stackTo)) {
                var foundSlot = slots.firstOrNull { it.slotIndex != slotTo.slotIndex && targetItem.equals(it.stack) }
                if (foundSlot == null) {
                    foundSlot = slots.firstOrNull { it.slotIndex != slotTo.slotIndex && targetItem.item == it.stack.item }
                }

                if (foundSlot != null) {
                    lastTask = moveItem(foundSlot, slotTo)
                    return
                } else {
                    checkSet.add(index)
                }
            }
        }
    }

    private fun moveItem(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return inventoryTask {
            priority(getModulePriority())
            pickUp(slotFrom)
            pickUp(slotTo)
            pickUp { event -> if (event.player.inventory.itemStack.isEmpty) null else slotFrom }
            runInGui()
            delay(clickDelay.toLong())
            postDelay(postDelay.toLong())
        }.also { InventoryTaskManager.addTask(it) }
    }
}
