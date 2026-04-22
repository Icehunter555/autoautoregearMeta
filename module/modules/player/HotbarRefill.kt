package dev.wizard.meta.module.modules.player

import dev.fastmc.common.ceilToInt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.operation.quickMove
import dev.wizard.meta.util.inventory.operation.moveTo
import dev.wizard.meta.util.inventory.slot.*
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

object HotbarRefill : Module(
    "Hotbar Refill",
    category = Category.PLAYER,
    description = "Automatically refills stackable items in your hotbar",
    modulePriority = 15
) {
    private val prioritizeCraftingSlot by setting(this, BooleanSetting(settingName("Prioritize Crafting Slot"), true))
    private val refillThreshold by setting(this, IntegerSetting(settingName("Refill Threshold"), 16, 1..63, 1))
    private val delayMs by setting(this, IntegerSetting(settingName("Delay ms"), 50, 0..1000, 5))

    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (lastTask?.isExecuted == false) return@safeParallelListener

            val sourceSlots = if (prioritizeCraftingSlot) {
                player.craftingSlots + player.storageSlots
            } else {
                player.storageSlots + player.craftingSlots
            }

            val targetSlots = player.hotbarSlots + player.offhandSlot

            for (slotTo in targetSlots.reversed()) {
                val stack = slotTo.stack
                if (stack.isEmpty || !stack.isStackable) continue

                if (stack.count < (stack.maxStackSize.toFloat() / 64.0f * refillThreshold.toFloat()).ceilToInt()) {
                    val slotFrom = sourceSlots.findFirstCompatibleStack(slotTo) ?: continue

                    lastTask = if (slotTo is HotbarSlot) {
                        InventoryTask.Builder().priority(getModulePriority()).build().apply {
                            quickMove(slotFrom)
                            runInGui()
                            postDelay(delayMs.toLong())
                            InventoryTaskManager.addTask(this)
                        }
                    } else {
                        InventoryTask.Builder().priority(getModulePriority()).build().apply {
                            moveTo(slotFrom, slotTo)
                            runInGui()
                            postDelay(delayMs.toLong())
                            InventoryTaskManager.addTask(this)
                        }
                    }
                    break
                }
            }
        }
    }
}
