package dev.wizard.meta.module.modules.beta

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.AutoArmor
import dev.wizard.meta.module.modules.combat.AutoRegear
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.operation.swapWith
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.firstEmpty
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.*
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand

object AutoArmorRegear : Module(
    "AutoArmorRegear",
    category = Category.BETA,
    description = "Automatically regear armor",
    modulePriority = 1999
) {
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 1, 0..20, 1))
    var moveTimeoutMs by setting(this, IntegerSetting(settingName("Move Timeout ms"), 100, 0..1000, 1))
    private val placeShulker by setting(this, BooleanSetting(settingName("Place Shulker"), false))
    private val closeShulker by setting(this, BooleanSetting(settingName("Close Shulker"), true, { placeShulker }))

    private var waited = 0
    private var actionSlot = 5
    var takingArmor = false
    private val moveTimeMap = Int2LongOpenHashMap().apply { defaultReturnValue(Long.MIN_VALUE) }
    private val armorTimer = TickTimer()
    private var lastTask: InventoryTask? = null
    private val timeoutTimer = TickTimer()
    private val closeTimer = TickTimer()

    init {
        safeListener<RunGameLoopEvent.Tick> {
            val openContainer = player.openContainer
            if (openContainer == player.inventoryContainer) return@safeListener

            if (!takeArmor(openContainer) && closeTimer.tickAndReset(5000, TimeUnit.MILLISECONDS) && closeShulker) {
                mc.displayGuiScreen(null)
            }
        }

        safeListener<TickEvent.Post> {
            if (waited++ < delay) return@safeListener

            for (hotbarIndex in 0..8) {
                val stack = player.inventory.getStackInSlot(hotbarIndex)
                if (stack.isEmpty) continue
                val item = stack.item
                if (item !is ItemArmor || stack.count != 127) continue

                val armorType = item.armorType
                val targetSlot = when (armorType) {
                    EntityEquipmentSlot.HEAD -> 5
                    EntityEquipmentSlot.CHEST -> 6
                    EntityEquipmentSlot.LEGS -> 7
                    EntityEquipmentSlot.FEET -> 8
                    else -> continue
                }
                swapStack(hotbarIndex + 36, targetSlot)
                break
            }
            waited = 0
        }

        onEnable {
            waited = 0
            actionSlot = 5
            if (placeShulker) {
                AutoRegear.placeShulker = true
            }
        }

        onDisable {
            waited = 0
            actionSlot = 5
        }
    }

    private fun SafeClientEvent.swapStack(slotFrom: Int, slotTo: Int) {
        val slot = slotFrom - 36
        mc.playerController.windowClick(0, slotTo, 1, ClickType.THROW, player)
        HotbarSwitchManager.ghostSwitch(this, slot) {
            player.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
        }
    }

    private fun SafeClientEvent.takeArmor(openContainer: Container): Boolean {
        AutoArmor.disable()
        val windowID = openContainer.windowId
        val currentTime = System.currentTimeMillis()
        val containerSlots = DefinedKt.getContainerSlots(openContainer).filter {
            currentTime > moveTimeMap.get(it.slotNumber)
        }

        var hotbarSlot = DefinedKt.getHotbarSlots(player).firstEmpty()
        if (hotbarSlot == null) {
            hotbarSlot = DefinedKt.getHotbarSlots(player).firstOrNull {
                val item = it.stack.item
                item !is ItemShulkerBox && item !is ItemArmor
            } ?: return false
        }

        val tempHotbarSlot = hotbarSlot
        for (slotFrom in containerSlots) {
            val stack = slotFrom.stack
            val item = stack.item
            if (item !is ItemArmor) continue
            if (!armorTimer.tickAndReset(100L)) {
                timeoutTimer.setTime(Long.MAX_VALUE)
                return true
            }

            InventoryTask.Builder().apply {
                priority(modulePriority)
                swapWith(windowID, slotFrom, tempHotbarSlot)
                delay(AutoRegear.clickDelayMs)
                postDelay(AutoRegear.postDelayMs)
                runInGui()
            }.build().also {
                InventoryTaskManager.addTask(it)
                lastTask = it
                moveTimeMap.put(slotFrom.slotNumber, currentTime + moveTimeoutMs)
                timeoutTimer.setTime(Long.MAX_VALUE)
            }
            return true
        }
        return false
    }

    private fun isValidItem(stack: ItemStack): Boolean {
        val item = stack.item
        return when {
            item is ItemSplashPotion -> false
            item == Items.TOTEM_OF_UNDYING -> false
            item is ItemPickaxe -> false
            item is ItemShulkerBox -> false
            item is ItemEnderPearl -> false
            item is ItemArmor -> stack.count >= 127
            else -> true
        }
    }

    data class InvStack(val slot: Int, val stack: ItemStack)
}
