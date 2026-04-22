package dev.wizard.meta.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.manager.managers.InventoryTaskManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.inventory.InventoryTask
import dev.wizard.meta.util.inventory.durability
import dev.wizard.meta.util.inventory.enchantmentLevel
import dev.wizard.meta.util.inventory.operation.pickUp
import dev.wizard.meta.util.inventory.operation.quickMove
import dev.wizard.meta.util.inventory.operation.swapWith
import dev.wizard.meta.util.inventory.slot.DefinedKt
import dev.wizard.meta.util.inventory.slot.HotbarSlot
import dev.wizard.meta.util.inventory.slot.toHotbarSlotOrNull
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import java.util.*
import kotlin.math.roundToInt

object AutoArmor : Module(
    "AutoArmor",
    category = Category.COMBAT,
    description = "Automatically equips armour",
    modulePriority = 500
) {
    var runInGui by setting(this, BooleanSetting(settingName("Run In GUI"), true))
    var antiGlitchArmor by setting(this, BooleanSetting(settingName("Anti Glitch Armor"), true))
    var stackedArmor by setting(this, BooleanSetting(settingName("Stacked Armor"), false))
    private val swapSlot by setting(this, IntegerSetting(settingName("Swap Slot"), 9, 1..9, 1, { stackedArmor }))
    private val blastProtectionLeggings by setting(this, BooleanSetting(settingName("Blast Protection Leggings"), true, description = "Prefer leggings with blast protection enchantment"))
    private val armorSaver by setting(this, BooleanSetting(settingName("Armor Saver"), false, { !stackedArmor }, description = "Swaps out armor at low durability"))
    private val duraThreshold by setting(this, IntegerSetting(settingName("Durability Threshold"), 10, 1..50, 1, { !stackedArmor && armorSaver }))
    private val delay by setting(this, IntegerSetting(settingName("Delay"), 1, 1..5, 1))
    private val armorCheckInterval by setting(this, IntegerSetting(settingName("Check Interval"), 5, 1..20, 1, description = "Ticks between armor checks (lower = faster)"))

    private val moveTimer = TickTimer(TimeUnit.TICKS)
    private val checkTimer = TickTimer(TimeUnit.TICKS)
    private var lastTask: InventoryTask? = null
    private val cachedArmorValues = mutableMapOf<ItemStack, Float>()
    private var lastInventoryHash = 0

    init {
        safeParallelListener<TickEvent.Post> {
            if (!runInGui && mc.currentScreen is GuiContainer || (lastTask != null && !lastTask!!.executed)) return@safeParallelListener
            if (!checkTimer.tick(armorCheckInterval.toLong())) return@safeParallelListener

            val armorSlots = DefinedKt.getArmorSlots(player)
            val isElytraOn = DefinedKt.getChestSlot(player).stack.item == Items.ELYTRA
            val currentHash = getInventoryHash()
            if (currentHash != lastInventoryHash) {
                cachedArmorValues.clear()
                lastInventoryHash = currentHash
            }

            val bestArmors = Array(4) { i -> armorSlots[i] to getArmorValue(armorSlots[i].stack) }
            findBestArmor(DefinedKt.getHotbarSlots(player), bestArmors, isElytraOn)
            findBestArmor(DefinedKt.getCraftingSlots(player), bestArmors, isElytraOn)
            findBestArmor(DefinedKt.getInventorySlots(player), bestArmors, isElytraOn)

            if (equipArmor(armorSlots, bestArmors)) {
                moveTimer.reset()
            } else if (antiGlitchArmor && moveTimer.tick(5)) {
                val currentArmorValue = player.totalArmorValue
                val rawArmorValue = armorSlots.sumBy { getRawArmorValue(it.stack) }
                if (currentArmorValue != rawArmorValue) {
                    InventoryTask.Builder().apply {
                        priority(modulePriority)
                        armorSlots.forEach {
                            pickUp(it)
                            pickUp(it)
                        }
                        delay(1, TimeUnit.TICKS)
                        postDelay(delay.toLong(), TimeUnit.TICKS)
                        runInGui()
                    }.build().let {
                        InventoryTaskManager.addTask(it)
                        moveTimer.reset()
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.getInventoryHash(): Int {
        var hash = 1
        for (slot in DefinedKt.getAllSlots(player)) {
            val stack = slot.stack
            if (stack.isEmpty || stack.item !is ItemArmor) continue
            hash = 31 * hash + stack.hashCode()
        }
        return hash
    }

    private fun getRawArmorValue(stack: ItemStack): Int {
        val item = stack.item
        return if (item is ItemArmor) item.damageReduceAmount else 0
    }

    private fun findBestArmor(slots: List<Slot>, bestArmors: Array<Pair<Slot, Float>>, isElytraOn: Boolean) {
        for (slot in slots) {
            val stack = slot.stack
            val item = stack.item
            if (item !is ItemArmor) continue
            val armorType = item.armorType
            if (armorType == EntityEquipmentSlot.CHEST && isElytraOn) continue
            val value = getArmorValue(stack)
            val index = 3 - armorType.index
            if (value > bestArmors[index].second) {
                bestArmors[index] = slot to value
            }
        }
    }

    private fun getArmorValue(stack: ItemStack): Float {
        if (stack.isEmpty) return -1.0f
        cachedArmorValues[stack]?.let { return it }

        val item = stack.item
        val value = if (item !is ItemArmor) -1.0f else {
            val baseValue = item.damageReduceAmount.toFloat() * getProtectionModifier(stack)
            if (!stackedArmor && armorSaver && stack.isItemDamaged && getDuraPercentage(stack) < duraThreshold) baseValue * 0.1f else baseValue
        }
        cachedArmorValues[stack] = value
        return value
    }

    private fun getDuraPercentage(stack: ItemStack): Int = if (stack.maxDamage == 0) 100 else (stack.durability.toFloat() / stack.maxDamage.toFloat() * 100.0f).roundToInt()

    private fun getProtectionModifier(stack: ItemStack): Float {
        val item = stack.item
        val protLevel = stack.enchantmentLevel(Enchantments.PROTECTION)
        val level = if (blastProtectionLeggings && item is ItemArmor && item.armorType == EntityEquipmentSlot.LEGS) {
            maxOf(stack.enchantmentLevel(Enchantments.BLAST_PROTECTION) * 2, protLevel)
        } else protLevel
        return 1.0f + 0.04f * level
    }

    private fun SafeClientEvent.equipArmor(armorSlots: List<Slot>, bestArmors: Array<Pair<Slot, Float>>): Boolean {
        for (i in bestArmors.indices) {
            val (slotFrom, _) = bestArmors[i]
            if (slotFrom.slotNumber in 5..8) continue
            val slotTo = armorSlots[i]
            lastTask = if (stackedArmor && slotFrom.stack.count > 1) moveStackedArmor(slotFrom, slotTo)
            else if (slotFrom.slotNumber in 1..4) moveFromCraftingSlot(slotFrom, slotTo)
            else moveFromInventory(slotFrom, slotTo)
            return true
        }
        return false
    }

    private fun SafeClientEvent.moveStackedArmor(slotFrom: Slot, slotTo: Slot): InventoryTask {
        slotFrom.toHotbarSlotOrNull()?.let { hotbarSlot ->
            return InventoryTask.Builder().apply {
                priority(modulePriority)
                if (slotTo.hasStack) {
                    pickUp(slotTo)
                    action { event -> HotbarSwitchManager.ghostSwitch(event, hotbarSlot) { event.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND)) } }
                    pickUp(slotFrom)
                } else {
                    action { event -> HotbarSwitchManager.ghostSwitch(event, hotbarSlot) { event.connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND)) } }
                }
                postDelay(delay.toLong(), TimeUnit.TICKS)
                runInGui()
            }.build().also { InventoryTaskManager.addTask(it) }
        }

        return InventoryTask.Builder().apply {
            priority(modulePriority)
            swapWith(slotFrom, DefinedKt.getHotbarSlot(player, swapSlot - 1))
            postDelay(delay.toLong(), TimeUnit.TICKS)
            runInGui()
        }.build().also { InventoryTaskManager.addTask(it) }
    }

    private fun moveFromCraftingSlot(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return InventoryTask.Builder().apply {
            priority(modulePriority)
            pickUp(slotFrom)
            pickUp(slotTo)
            if (slotTo.hasStack) pickUp(slotFrom)
            postDelay(delay.toLong(), TimeUnit.TICKS)
            runInGui()
        }.build().also { InventoryTaskManager.addTask(it) }
    }

    private fun SafeClientEvent.moveFromInventory(slotFrom: Slot, slotTo: Slot): InventoryTask {
        return InventoryTask.Builder().apply {
            priority(modulePriority)
            if (!slotTo.hasStack) {
                quickMove(slotFrom)
            } else if (DefinedKt.getInventorySlots(player).any { !it.hasStack }) {
                quickMove(slotTo)
                quickMove(slotFrom)
            } else {
                pickUp(slotFrom)
                pickUp(slotTo)
            }
            postDelay(delay.toLong(), TimeUnit.TICKS)
            runInGui()
        }.build().also { InventoryTaskManager.addTask(it) }
    }
}
