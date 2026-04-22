package dev.wizard.meta.gui.hudgui.elements.hud

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.inventory.slot.allSlotsPrioritized
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionUtils

object LowItems : LabelHud("LowItems", category = Category.TEXT, description = "Warns when low on splash potions, beds, shulker boxes, armor, or totems") {

    private val potionThreshold by setting(this, "Potion Threshold", 64, 10..128, 1)
    private val bedThreshold by setting(this, "Bed Threshold", 64, 10..128, 1)
    private val shulkerThreshold by setting(this, "Shulker Threshold", 32, 10..128, 1)
    private val armorThreshold by setting(this, "Armor Threshold", 39, 10..128, 1)
    private val totemThreshold by setting(this, "Totem Threshold", 1, 1..5, 1)

    private val showPotionWarning by setting(this, "Show Potion Warning", true)
    private val showBedWarning by setting(this, "Show Bed Warning", true)
    private val bedsNotInOverworld by setting(this, "Beds only when valid", false)
    private val showShulkerWarning by setting(this, "Show Shulker Warning", true)
    private val showArmorWarning by setting(this, "Show Armor Warning", true)
    private val armorTypeFilter by setting(this, "Armor Type", ArmorType.DIAMOND_ONLY)
    private val showTotemWarning by setting(this, "Show Totem Warning", true)
    private val countOffhandTotem by setting(this, "Count Offhand Totem", true)

    private val fcolor1 by setting(this, "Text Color", ColorRGB(255, 255, 255))
    private val fcolor2 by setting(this, "Flash Color", ColorRGB(200, 0, 0))
    private val flashspeed by setting(this, "Flash Delay", 400, 0..500, 10)

    private val updateTimer = TickTimer()
    private val flashTimer = TickTimer()
    private var isFlashColorToggle = false

    private var cachedPotionCount = 0
    private var cachedBedCount = 0
    private var cachedShulkerCount = 0
    private var cachedArmorCounts = mutableMapOf<String, Int>()
    private var cachedTotemCount = 0

    override fun updateText(event: SafeClientEvent) {
        if (updateTimer.tickAndReset(5)) {
            cachedPotionCount = countInstantHealthPotions(event)
            cachedBedCount = countBeds(event)
            cachedShulkerCount = countShulkerBoxes(event)
            cachedArmorCounts = countArmorByType(event).toMutableMap()
            cachedTotemCount = countTotems(event)
        }

        if (flashTimer.tickAndReset(flashspeed)) {
            isFlashColorToggle = !isFlashColorToggle
        }

        val flashColor = if (isFlashColorToggle) fcolor2.unbox() else fcolor1.unbox()
        displayText.clear()

        if (showPotionWarning && cachedPotionCount < potionThreshold) {
            displayText.addLine("Low on pots! ($cachedPotionCount/$potionThreshold)", flashColor)
        }
        if (showBedWarning && cachedBedCount < bedThreshold && (!bedsNotInOverworld || event.player.dimension != 0)) {
            displayText.addLine("Low on beds! ($cachedBedCount/$bedThreshold)", flashColor)
        }
        if (showShulkerWarning && cachedShulkerCount < shulkerThreshold) {
            displayText.addLine("Low on shulkers! ($cachedShulkerCount/$shulkerThreshold)", flashColor)
        }
        if (showArmorWarning) {
            val lowArmorPieces = cachedArmorCounts.filter { it.value < armorThreshold }
            lowArmorPieces.forEach { (armorType, count) ->
                displayText.addLine("Low on $armorType! ($count/$armorThreshold)", flashColor)
            }
        }
        if (showTotemWarning && cachedTotemCount < totemThreshold) {
            displayText.addLine("Low on totems! ($cachedTotemCount/$totemThreshold)", flashColor)
        }
    }

    private fun countInstantHealthPotions(event: SafeClientEvent): Int {
        return event.player.allSlotsPrioritized.sumOf { slot ->
            val stack = slot.stack
            if (isInstantHealthPotion(stack)) stack.count else 0
        }
    }

    private fun countBeds(event: SafeClientEvent): Int {
        return event.player.allSlotsPrioritized.sumOf { slot ->
            val stack = slot.stack
            if (isBed(stack)) stack.count else 0
        }
    }

    private fun isInstantHealthPotion(stack: ItemStack): Boolean {
        if (stack.item != Items.SPLASH_POTION) return false
        val effects = PotionUtils.getEffectsFromStack(stack)
        return effects.any { it.potion == MobEffects.INSTANT_HEALTH }
    }

    private fun isBed(stack: ItemStack): Boolean {
        if (stack.item == Items.BED) return true
        val registryName = stack.item.registryName?.toString()
        return registryName != null && (registryName.endsWith("_bed") || registryName == "minecraft:bed")
    }

    private fun countShulkerBoxes(event: SafeClientEvent): Int {
        return event.player.allSlotsPrioritized.sumOf { slot ->
            val stack = slot.stack
            if (!stack.isEmpty && stack.item is ItemShulkerBox) stack.count else 0
        }
    }

    private fun countArmorByType(event: SafeClientEvent): Map<String, Int> {
        return when (armorTypeFilter) {
            ArmorType.DIAMOND_ONLY -> countDiamondArmorByType(event)
            ArmorType.ALL_ARMOR -> countAllArmorByType(event)
        }
    }

    private fun countDiamondArmorByType(event: SafeClientEvent): Map<String, Int> {
        val armorCounts = mutableMapOf("helmets" to 0, "chestplates" to 0, "leggings" to 0, "boots" to 0)
        event.player.allSlotsPrioritized.forEach { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@forEach
            val item = stack.item
            when (item) {
                Items.DIAMOND_HELMET -> armorCounts["helmets"] = armorCounts["helmets"]!! + stack.count
                Items.DIAMOND_CHESTPLATE -> armorCounts["chestplates"] = armorCounts["chestplates"]!! + stack.count
                Items.DIAMOND_LEGGINGS -> armorCounts["leggings"] = armorCounts["leggings"]!! + stack.count
                Items.DIAMOND_BOOTS -> armorCounts["boots"] = armorCounts["boots"]!! + stack.count
            }
        }
        return armorCounts
    }

    private fun countAllArmorByType(event: SafeClientEvent): Map<String, Int> {
        val armorCounts = mutableMapOf("helmets" to 0, "chestplates" to 0, "leggings" to 0, "boots" to 0)
        event.player.allSlotsPrioritized.forEach { slot ->
            val stack = slot.stack
            if (stack.isEmpty) return@forEach
            val item = stack.item
            if (item == Items.DIAMOND_HELMET || item == Items.GOLDEN_HELMET || item == Items.IRON_HELMET || item == Items.CHAINMAIL_HELMET || item == Items.LEATHER_HELMET) {
                armorCounts["helmets"] = armorCounts["helmets"]!! + stack.count
            } else if (item == Items.DIAMOND_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE || item == Items.IRON_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE || item == Items.LEATHER_CHESTPLATE) {
                armorCounts["chestplates"] = armorCounts["chestplates"]!! + stack.count
            } else if (item == Items.DIAMOND_LEGGINGS || item == Items.GOLDEN_LEGGINGS || item == Items.IRON_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS || item == Items.LEATHER_LEGGINGS) {
                armorCounts["leggings"] = armorCounts["leggings"]!! + stack.count
            } else if (item == Items.DIAMOND_BOOTS || item == Items.GOLDEN_BOOTS || item == Items.IRON_BOOTS || item == Items.CHAINMAIL_BOOTS || item == Items.LEATHER_BOOTS) {
                armorCounts["boots"] = armorCounts["boots"]!! + stack.count
            }
        }
        return armorCounts
    }

    private fun countTotems(event: SafeClientEvent): Int {
        var count = event.player.allSlotsPrioritized.sumOf { slot ->
            val stack = slot.stack
            if (!stack.isEmpty && stack.item == Items.TOTEM_OF_UNDYING) stack.count else 0
        }
        if (countOffhandTotem && !event.player.heldItemOffhand.isEmpty && event.player.heldItemOffhand.item == Items.TOTEM_OF_UNDYING) {
            count += event.player.heldItemOffhand.count
        }
        return count
    }

    private enum class ArmorType {
        DIAMOND_ONLY, ALL_ARMOR
    }
}
