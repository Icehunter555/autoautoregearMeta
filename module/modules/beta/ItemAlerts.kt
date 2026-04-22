package dev.wizard.meta.module.modules.beta

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.Alerts
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.accessor.inPortal
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.slot.DefinedKt
import net.minecraft.block.Block
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.*
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionUtils
import net.minecraft.util.SoundEvent

object ItemAlerts : Module(
    "ItemAlerts",
    category = Category.BETA,
    description = "alerts when your low on items",
    modulePriority = 500
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.GENERAL))
    private val priority1CheckDelay by setting(this, IntegerSetting(settingName("Priority 1 Check Delay"), 1, 1..10, 1, { page == Page.GENERAL }))
    private val priority2CheckDelay by setting(this, IntegerSetting(settingName("Priority 2 Check Delay"), 3, 1..10, 1, { page == Page.GENERAL }))
    private val priority3CheckDelay by setting(this, IntegerSetting(settingName("Priority 3 Check Delay"), 5, 1..10, 1, { page == Page.GENERAL }))
    private val zeroCheckDelay by setting(this, IntegerSetting(settingName("Zero Items Alert Delay"), 6, 1..10, 1, { page == Page.GENERAL }))
    private val checkWhenNaked by setting(this, BooleanSetting(settingName("Check When Naked"), false, { page == Page.GENERAL }))

    private val totemMode by setting(this, EnumSetting(settingName("Totem Mode"), ItemMode.ALWAYS, { page == Page.PRIORITY1 }))
    private val totemThreshold by setting(this, IntegerSetting(settingName("Totem Threshold"), 3, 1..8, 1, { totemMode != ItemMode.DISABLED && page == Page.PRIORITY1 }))
    private val totemSound by setting(this, BooleanSetting(settingName("Totem Sound"), true, { totemMode != ItemMode.DISABLED && page == Page.PRIORITY1 }))

    private val potionMode by setting(this, EnumSetting(settingName("Health Potion Mode"), ItemMode.ALWAYS, { page == Page.PRIORITY1 }))
    private val healthPotionThreshold by setting(this, IntegerSetting(settingName("Health Potion Threshold"), 70, 10..128, 1, { potionMode != ItemMode.DISABLED && page == Page.PRIORITY1 }))
    private val healthPotionSound by setting(this, BooleanSetting(settingName("Health Potion Sound"), true, { potionMode != ItemMode.DISABLED && page == Page.PRIORITY1 }))

    private val armorMode by setting(this, EnumSetting(settingName("Armor Mode"), ItemMode.ALWAYS, { page == Page.PRIORITY1 }))
    private val helmetThreshold by setting(this, IntegerSetting(settingName("Helmet Threshold"), 25, 1..64, 1, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val chestThreshold by setting(this, IntegerSetting(settingName("Chestplate Threshold"), 20, 1..64, 1, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val leggingThreshold by setting(this, IntegerSetting(settingName("Legging Threshold"), 25, 1..64, 1, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val bootThreshold by setting(this, IntegerSetting(settingName("Boot Threshold"), 25, 1..64, 1, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val ignoreElytra by setting(this, BooleanSetting(settingName("Ignore Elytra"), true, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val armorValid by setting(this, EnumSetting(settingName("Armor Mode"), ArmorValid.DIAMONDONLY, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val checkInventory by setting(this, BooleanSetting(settingName("Check Inventory Slots"), false, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))
    private val armorSound by setting(this, BooleanSetting(settingName("Armor Sound"), false, { page == Page.PRIORITY1 && armorMode != ItemMode.DISABLED }))

    private val regearMode by setting(this, EnumSetting(settingName("Regear Mode"), ItemMode.ALWAYS, { page == Page.PRIORITY1 }))
    private val regearThreshold by setting(this, IntegerSetting(settingName("Regear Threshold"), 20, 1..64, 1, { page == Page.PRIORITY1 && regearMode != ItemMode.DISABLED }))
    private val regearSound by setting(this, BooleanSetting(settingName("Regear Sound"), false, { page == Page.PRIORITY1 && regearMode != ItemMode.DISABLED }))

    private val bedsMode by setting(this, EnumSetting(settingName("Beds Mode"), ItemMode.NETHERONLY, { page == Page.PRIORITY2 }))
    private val bedThreshold by setting(this, IntegerSetting(settingName("Bed Threshold"), 64, 1..120, 1, { bedsMode != ItemMode.DISABLED && page == Page.PRIORITY2 }))
    private val bedSound by setting(this, BooleanSetting(settingName("Bed Sound"), false, { bedsMode != ItemMode.DISABLED && page == Page.PRIORITY2 }))

    private val crystalMode by setting(this, EnumSetting(settingName("Crystal Mode"), ItemMode.NOTNETHER, { page == Page.PRIORITY2 }))
    private val crystalThreshold by setting(this, IntegerSetting(settingName("Crystal Threshold"), 64, 1..120, 1, { crystalMode != ItemMode.DISABLED && page == Page.PRIORITY2 }))
    private val crystalSound by setting(this, BooleanSetting(settingName("Crystal Sound"), false, { crystalMode != ItemMode.DISABLED && page == Page.PRIORITY2 }))

    private val gappleMode by setting(this, EnumSetting(settingName("Gapple Mode"), ItemMode.ALWAYS, { page == Page.PRIORITY2 }))
    private val gappleThreshold by setting(this, IntegerSetting(settingName("Gapple Threshold"), 20, 1..64, 1, { gappleMode != ItemMode.DISABLED && page == Page.PRIORITY2 }))
    private val gappleSound by setting(this, BooleanSetting(settingName("Gapple Sound"), false, { page == Page.PRIORITY2 && gappleMode != ItemMode.DISABLED }))

    private val obsidianMode by setting(this, EnumSetting(settingName("Obsidian Mode"), ItemMode.ALWAYS, { page == Page.PRIORITY2 }))
    private val obsidianThreshold by setting(this, IntegerSetting(settingName("Obsidian Threshold"), 20, 1..128, 1, { obsidianMode != ItemMode.DISABLED && page == Page.PRIORITY2 }))
    private val obsidianSound by setting(this, BooleanSetting(settingName("Obsidian Sound"), false, { page == Page.PRIORITY2 && obsidianMode != ItemMode.DISABLED }))

    private val pearlMode by setting(this, EnumSetting(settingName("Pearl Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 }))
    private val pearlThreshold by setting(this, IntegerSetting(settingName("Pearl Threshold"), 10, 1..64, 1, { page == Page.PRIORITY3 && pearlMode != ItemMode.DISABLED }))
    private val pearlSound by setting(this, BooleanSetting(settingName("Pearl Sound"), false, { page == Page.PRIORITY3 && pearlMode != ItemMode.DISABLED }))

    private val pistonMode by setting(this, EnumSetting(settingName("Piston Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 }))
    private val pistonThreshold by setting(this, IntegerSetting(settingName("Piston Threshold"), 10, 1..64, 1, { page == Page.PRIORITY3 && pistonMode != ItemMode.DISABLED }))
    private val powerThreshold by setting(this, IntegerSetting(settingName("Power Threshold"), 10, 1..64, 1, { page == Page.PRIORITY3 && pistonMode != ItemMode.DISABLED }))
    private val pistonSound by setting(this, BooleanSetting(settingName("Piston Sound"), false, { page == Page.PRIORITY3 && pistonMode != ItemMode.DISABLED }))
    private val redstoneSound by setting(this, BooleanSetting(settingName("Redstone Sound"), false, { page == Page.PRIORITY3 && pistonMode != ItemMode.DISABLED }))

    private val tntMinecartMode by setting(this, EnumSetting(settingName("TNT Minecart Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 }))
    private val tntMinecartThreshold by setting(this, IntegerSetting(settingName("TNT Minecart Threshold"), 30, 1..128, 1, { page == Page.PRIORITY3 && tntMinecartMode != ItemMode.DISABLED }))
    private val railThreshold by setting(this, IntegerSetting(settingName("Rail Threshold"), 15, 1..64, 1, { page == Page.PRIORITY3 && tntMinecartMode != ItemMode.DISABLED }))
    private val tntMinecartSound by setting(this, BooleanSetting(settingName("TNT Minecart Sound"), false, { page == Page.PRIORITY3 && tntMinecartMode != ItemMode.DISABLED }))
    private val railSound by setting(this, BooleanSetting(settingName("Rail Sound"), false, { page == Page.PRIORITY3 && tntMinecartMode != ItemMode.DISABLED }))

    private val skullMode by setting(this, EnumSetting(settingName("Skull Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 }))
    private val skullThreshold by setting(this, IntegerSetting(settingName("Skull Threshold"), 20, 1..64, 1, { page == Page.PRIORITY3 && skullMode != ItemMode.DISABLED }))
    private val skullSound by setting(this, BooleanSetting(settingName("Skull Sound"), false, { page == Page.PRIORITY3 && skullMode != ItemMode.DISABLED }))

    private val sandMode by setting(this, EnumSetting(settingName("Sand Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 }))
    private val sandThreshold by setting(this, IntegerSetting(settingName("Sand Threshold"), 20, 1..64, 1, { page == Page.PRIORITY3 && sandMode != ItemMode.DISABLED }))
    private val sandSound by setting(this, BooleanSetting(settingName("Sand Sound"), false, { page == Page.PRIORITY3 && sandMode != ItemMode.DISABLED }))

    private val speedPotionMode by setting(this, EnumSetting(settingName("Speed Potion Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 }))
    private val speedPotionThreshold by setting(this, IntegerSetting(settingName("Speed Potion Threshold"), 20, 1..64, 1, { page == Page.PRIORITY3 && speedPotionMode != ItemMode.DISABLED }))
    private val speedPotionSound by setting(this, BooleanSetting(settingName("Speed Potion Sound"), false, { page == Page.PRIORITY3 && speedPotionMode != ItemMode.DISABLED }))

    private val addEnderChestsToObsidian by setting(this, BooleanSetting(settingName("Add EnderChests to Obsidian"), false, { page == Page.PRIORITY3 || page == Page.PRIORITY2 }))
    private val enderChestMode by setting(this, EnumSetting(settingName("Ender Chest Mode"), ItemMode.DISABLED, { page == Page.PRIORITY3 && !addEnderChestsToObsidian }))
    private val enderChestThreshold by setting(this, IntegerSetting(settingName("Ender Chest Threshold"), 20, 1..64, 1, { page == Page.PRIORITY3 && enderChestMode != ItemMode.DISABLED && !addEnderChestsToObsidian }))
    private val enderChestSound by setting(this, BooleanSetting(settingName("Ender Chest Sound"), false, { page == Page.PRIORITY3 && enderChestMode != ItemMode.DISABLED && !addEnderChestsToObsidian }))

    private var zeroList = mutableListOf<Item>()
    private val prio1CheckTimer = TickTimer(TimeUnit.SECONDS)
    private val prio2CheckTimer = TickTimer(TimeUnit.SECONDS)
    private val prio3CheckTimer = TickTimer(TimeUnit.SECONDS)
    private val zeroCheckTimer = TickTimer(TimeUnit.SECONDS)

    init {
        onDisable {
            zeroList.clear()
            prio1CheckTimer.reset(100)
            prio2CheckTimer.reset(100)
            prio3CheckTimer.reset(100)
            zeroCheckTimer.reset(100)
        }

        safeListener<TickEvent.Pre> {
            if (!shouldRunChecks()) return@safeListener

            if (prio1CheckTimer.tickAndReset(priority1CheckDelay.toLong())) {
                handlePotion(MobEffects.INSTANT_HEALTH)
                handleRegears()
                handleAllArmor()
                handleStandardItem(Items.TOTEM_OF_UNDYING)
            }

            if (prio2CheckTimer.tickAndReset(priority2CheckDelay.toLong())) {
                handleGroupedItems(2)
            }

            if (prio3CheckTimer.tickAndReset(priority3CheckDelay.toLong())) {
                handleGroupedItems(3)
                handlePotion(MobEffects.SPEED)
            }

            if (zeroCheckTimer.tickAndReset(zeroCheckDelay.toLong())) {
                zeroList.filter { it != Item.getItemFromBlock(Blocks.ENDER_CHEST) || !addEnderChestsToObsidian }.forEach {
                    handleZeroItem(it)
                }
            }
        }
    }

    private fun SafeClientEvent.shouldRunChecks(): Boolean {
        val itemCount = DefinedKt.getAllSlotsPrioritized(player).count { it.hasStack }
        return !player.inPortal && (itemCount > 8 || checkWhenNaked)
    }

    private fun SafeClientEvent.checkArmorCount(armorItem: Item): Int {
        return if (checkInventory) {
            getItemCount(armorItem)
        } else {
            player.armorInventoryList.filter { it.item == armorItem }.sumBy { it.count }
        }
    }

    private fun isValidArmor(armorItem: Item): Boolean {
        if (armorItem !is ItemArmor) {
            return ignoreElytra && armorItem == Items.ELYTRA
        }
        return armorItem.armorMaterial == ItemArmor.ArmorMaterial.DIAMOND || armorValid == ArmorValid.ANY
    }

    private fun SafeClientEvent.handleGroupedItems(priority: Int) {
        val items = when (priority) {
            2 -> listOf(Items.PEARL, Items.END_CRYSTAL, Items.GOLDEN_APPLE, Item.getItemFromBlock(Blocks.OBSIDIAN))
            3 -> listOfNotNull(Items.CHORUS_FRUIT, Item.getItemFromBlock(Blocks.PISTON), Item.getItemFromBlock(Blocks.REDSTONE_BLOCK), Items.TNT_MINECART, Item.getItemFromBlock(Blocks.RAIL), Item.getItemFromBlock(Blocks.SKULL), Item.getItemFromBlock(Blocks.CONCRETE_POWDER), if (!addEnderChestsToObsidian) Item.getItemFromBlock(Blocks.ENDER_CHEST) else null)
            else -> return
        }

        val lowItems = mutableListOf<ItemAlert>()
        val zeroItems = mutableListOf<String>()
        var playSound = false

        for (item in items) {
            val mode = getModeFromItem(item)
            if (mode == ItemMode.DISABLED || (mode == ItemMode.NETHERONLY && player.dimension == 0) || (mode == ItemMode.NOTNETHER && player.dimension == -1)) continue

            val count = if (item == Item.getItemFromBlock(Blocks.OBSIDIAN) && addEnderChestsToObsidian) {
                getItemCount(item) + getItemCount(Item.getItemFromBlock(Blocks.ENDER_CHEST)) * 8
            } else {
                getItemCount(item)
            }

            val threshold = getThresholdFromItem(item)
            val sound = isItemSoundEnabled(item)

            if (zeroList.contains(item) && count != 0) {
                zeroList.remove(item)
            } else if (zeroList.contains(item) && count == 0) continue

            if (!zeroList.contains(item) && count == 0) {
                zeroList.add(item)
                zeroItems.add(getNameFromItem(item))
                if (sound) playSound = true
                continue
            }

            if (count <= threshold) {
                lowItems.add(ItemAlert(item, count, threshold, sound))
                if (sound) playSound = true
            }
        }

        val lowString = when (lowItems.size) {
            0 -> ""
            1 -> "You are low on ${getNameFromItem(lowItems[0].item)} (${lowItems[0].count}/${lowItems[0].threshold})"
            2 -> "You are low on ${getNameFromItem(lowItems[0].item)} (${lowItems[0].count}/${lowItems[0].threshold}) and ${getNameFromItem(lowItems[1].item)} (${lowItems[1].count}/${lowItems[1].threshold})"
            else -> {
                val notLast = lowItems.dropLast(1).joinToString(", ") { "${getNameFromItem(it.item)} (${it.count}/${it.threshold})" }
                "You are low on $notLast, and ${getNameFromItem(lowItems.last().item)} (${lowItems.last().count}/${lowItems.last().threshold})"
            }
        }

        val zeroString = when (zeroItems.size) {
            0 -> ""
            1 -> "ou are out of ${zeroItems[0]}!"
            2 -> "ou are out of ${zeroItems[0]} and ${zeroItems[1]}!"
            else -> {
                val notLast = zeroItems.dropLast(1).joinToString(", ")
                "ou are out of $notLast, and ${zeroItems.last()}!"
            }
        }

        val finalString = if (lowString.isEmpty() && zeroString.isNotEmpty()) "Y$zeroString"
        else if (zeroString.isEmpty() && lowString.isNotEmpty()) "$lowString!"
        else if (lowString.isNotEmpty() && zeroString.isNotEmpty()) "$lowString, and y$zeroString"
        else ""

        if (finalString.isNotEmpty()) {
            Alerts.sendAlert(finalString, if (zeroItems.isNotEmpty()) priority - 1 else priority + 1)
            if (playSound) {
                val soundItem = lowItems.firstOrNull()?.item ?: items.firstOrNull { zeroList.contains(it) }
                soundItem?.let {
                    mc.soundHandler.playSound(PositionedSoundRecord.getRecord(getSoundFromItem(it), 1.0f, 1.0f))
                }
            }
        }
    }

    private fun SafeClientEvent.handleRegears() {
        if (regearMode == ItemMode.DISABLED || (regearMode == ItemMode.NETHERONLY && player.dimension == 0) || (regearMode == ItemMode.NOTNETHER && player.dimension == -1)) return
        val regearCount = DefinedKt.getAllSlotsPrioritized(player).filter { it.stack.item is ItemShulkerBox }.sumBy { it.stack.count }
        if (regearCount <= regearThreshold) {
            if (regearCount != 0) {
                Alerts.sendAlert("You are low on regears! Only $regearCount remaining!", 1)
                if (regearSound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_SHULKER_TELEPORT, 1.0f, 1.0f))
            } else {
                Alerts.sendAlert("You are out of regears! RESTOCK IMMEDIATELY!", 1)
                mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 1.0f))
            }
        }
    }

    private fun SafeClientEvent.handleAllArmor() {
        if (armorMode == ItemMode.DISABLED || (armorMode == ItemMode.NETHERONLY && player.dimension == 0) || (armorMode == ItemMode.NOTNETHER && player.dimension == -1)) return
        val invalidList = mutableMapOf<String, Int>()

        val helmCount = getArmorFromSlot(EntityEquipmentSlot.HEAD).sumBy { checkArmorCount(it) }
        val chestCount = getArmorFromSlot(EntityEquipmentSlot.CHEST).sumBy { checkArmorCount(it) }
        val legsCount = getArmorFromSlot(EntityEquipmentSlot.LEGS).sumBy { checkArmorCount(it) }
        val bootsCount = getArmorFromSlot(EntityEquipmentSlot.FEET).sumBy { checkArmorCount(it) }

        if (getArmorFromSlot(EntityEquipmentSlot.HEAD).none { checkArmorCount(it) > helmetThreshold && isValidArmor(it) }) invalidList["helmets"] = helmCount
        if (getArmorFromSlot(EntityEquipmentSlot.CHEST).none { checkArmorCount(it) > chestThreshold && isValidArmor(it) }) invalidList["chestplates"] = chestCount
        if (getArmorFromSlot(EntityEquipmentSlot.LEGS).none { checkArmorCount(it) > leggingThreshold && isValidArmor(it) }) invalidList["leggings"] = legsCount
        if (getArmorFromSlot(EntityEquipmentSlot.FEET).none { checkArmorCount(it) > bootThreshold && isValidArmor(it) }) invalidList["boots"] = bootsCount

        if (invalidList.isEmpty()) return

        val nonZero = invalidList.filter { it.value != 0 }.toList()
        val isZero = invalidList.filter { it.value == 0 }.keys.toList()

        val lowString = when (nonZero.size) {
            0 -> ""
            1 -> "You are low on ${nonZero[0].first} (${nonZero[0].second})"
            2 -> "You are low on ${nonZero[0].first} (${nonZero[0].second}) and ${nonZero[1].first} (${nonZero[1].second})"
            else -> {
                val notLast = nonZero.dropLast(1).joinToString(", ") { "${it.first} (${it.second})" }
                "You are low on $notLast, and ${nonZero.last().first} (${nonZero.last().second})"
            }
        }

        val zeroString = when (isZero.size) {
            0 -> ""
            1 -> "ou are out of ${isZero[0]}! REGEAR IMMEDIATELY!"
            2 -> "ou are out of ${isZero[0]} and ${isZero[1]}! REGEAR IMMEDIATELY!"
            else -> {
                val notLast = isZero.dropLast(1).joinToString(", ")
                "ou are out of $notLast, and ${isZero.last()}! REGEAR IMMEDIATELY!"
            }
        }

        val finalString = if (lowString.isEmpty() && zeroString.isNotEmpty()) "Y$zeroString"
        else if (zeroString.isEmpty() && lowString.isNotEmpty()) "$lowString!"
        else if (lowString.isNotEmpty() && zeroString.isNotEmpty()) "$lowString, and y$zeroString"
        else ""

        Alerts.sendAlert(finalString, if (isZero.isNotEmpty()) 1 else 2)
        if (armorSound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f))
    }

    private fun getArmorFromSlot(slot: EntityEquipmentSlot): List<Item> {
        val returnList = mutableListOf<Item>()
        when (slot) {
            EntityEquipmentSlot.FEET -> {
                returnList.add(Items.DIAMOND_BOOTS)
                if (armorValid == ArmorValid.ANY) returnList.addAll(listOf(Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS))
            }
            EntityEquipmentSlot.LEGS -> {
                returnList.add(Items.DIAMOND_LEGGINGS)
                if (armorValid == ArmorValid.ANY) returnList.addAll(listOf(Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS))
            }
            EntityEquipmentSlot.CHEST -> {
                returnList.add(Items.DIAMOND_CHESTPLATE)
                if (armorValid == ArmorValid.ANY) returnList.addAll(listOf(Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE))
            }
            EntityEquipmentSlot.HEAD -> {
                returnList.add(Items.DIAMOND_HELMET)
                if (armorValid == ArmorValid.ANY) returnList.addAll(listOf(Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET))
            }
            else -> {}
        }
        return returnList
    }

    private fun SafeClientEvent.countPotions(potion: Potion): Int {
        return DefinedKt.getAllSlotsPrioritized(player).filter {
            val stack = it.stack
            !stack.isEmpty && stack.item is ItemSplashPotion && PotionUtils.getEffectsFromStack(stack).any { it.potion == potion }
        }.sumBy { it.stack.count }
    }

    private fun SafeClientEvent.handlePotion(potion: Potion) {
        val count = countPotions(potion)
        if (potion == MobEffects.INSTANT_HEALTH) {
            if (potionMode == ItemMode.DISABLED || (potionMode == ItemMode.NETHERONLY && player.dimension == 0) || (potionMode == ItemMode.NOTNETHER && player.dimension == -1)) return
            if (count <= healthPotionThreshold) {
                if (count != 0) {
                    Alerts.sendAlert("Low on health potions! Only $count remaining!", 2)
                    if (healthPotionSound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f))
                } else {
                    Alerts.sendAlert("Out of health potions! REGEAR IMMEDIATELY!", 1)
                    mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_WITHER_DEATH, 1.0f, 1.0f))
                }
            }
        } else if (potion == MobEffects.SPEED) {
            if (speedPotionMode == ItemMode.DISABLED || (speedPotionMode == ItemMode.NETHERONLY && player.dimension == 0) || (speedPotionMode == ItemMode.NOTNETHER && player.dimension == -1)) return
            if (count <= speedPotionThreshold) {
                if (count != 0) {
                    Alerts.sendAlert("Low on speed potions! Only $count remaining!", 4)
                    if (speedPotionSound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f))
                } else {
                    Alerts.sendAlert("Out of speed potions!", 3)
                    if (speedPotionSound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f))
                }
            }
        }
    }

    private fun SafeClientEvent.handleStandardItem(item: Item) {
        val threshold = getThresholdFromItem(item)
        val mode = getModeFromItem(item)
        val sound = isItemSoundEnabled(item)
        if (mode == ItemMode.DISABLED || (mode == ItemMode.NETHERONLY && player.dimension == 0) || (mode == ItemMode.NOTNETHER && player.dimension == -1)) return

        val count = getItemCount(item)
        if (zeroList.contains(item) && count != 0) {
            zeroList.remove(item)
        } else if (zeroList.contains(item) && count == 0) return

        if (!zeroList.contains(item) && count == 0) {
            zeroList.add(item)
            handleZeroItem(item)
            return
        }

        if (count <= threshold) {
            Alerts.sendAlert("Low on ${getNameFromItem(item)}! Only $count remaining!", getPriorityFromItem(item, false))
            if (sound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(getSoundFromItem(item), 1.0f, 1.0f))
            if (count == 0) zeroList.add(item)
        }
    }

    private fun SafeClientEvent.handleZeroItem(item: Item) {
        val sound = isItemSoundEnabled(item)
        val mode = getModeFromItem(item)
        if (mode == ItemMode.DISABLED || (mode == ItemMode.NETHERONLY && player.dimension == 0) || (mode == ItemMode.NOTNETHER && player.dimension == -1)) return
        Alerts.sendAlert("No ${getNameFromItem(item)} left!", getPriorityFromItem(item, true))
        if (sound) mc.soundHandler.playSound(PositionedSoundRecord.getRecord(getSoundFromItem(item), 1.0f, 1.0f))
    }

    private fun getNameFromItem(item: Item): String {
        return when {
            item == Items.TOTEM_OF_UNDYING -> "totems"
            item is ItemShulkerBox -> "regears"
            item is ItemSplashPotion -> {
                val effects = PotionUtils.getEffectsFromStack(ItemStack(item))
                if (effects.any { it.potion == MobEffects.INSTANT_HEALTH }) "health potions"
                else if (effects.any { it.potion == MobEffects.SPEED }) "speed potions"
                else ""
            }
            item is ItemBed -> "beds"
            item == Items.END_CRYSTAL -> "crystals"
            item == Items.GOLDEN_APPLE -> "gapples"
            item == Item.getItemFromBlock(Blocks.OBSIDIAN) -> "obsidian"
            item == Items.PEARL -> "pearls"
            item is ItemPiston -> "pistons"
            item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) -> "redstone"
            item == Items.TNT_MINECART -> "carts"
            item == Item.getItemFromBlock(Blocks.RAIL) -> "rails"
            item is ItemSkull -> "skulls"
            item == Item.getItemFromBlock(Blocks.CONCRETE_POWDER) -> "concrete powder"
            item == Item.getItemFromBlock(Blocks.ENDER_CHEST) -> "ender chests"
            else -> ""
        }
    }

    private fun getModeFromItem(item: Item): ItemMode {
        return when {
            item == Items.TOTEM_OF_UNDYING -> totemMode
            item is ItemBed -> bedsMode
            item == Items.END_CRYSTAL -> crystalMode
            item == Items.GOLDEN_APPLE -> gappleMode
            item == Item.getItemFromBlock(Blocks.OBSIDIAN) -> obsidianMode
            item == Items.PEARL -> pearlMode
            item is ItemPiston -> pistonMode
            item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) -> pistonMode
            item == Items.TNT_MINECART -> tntMinecartMode
            item == Item.getItemFromBlock(Blocks.RAIL) -> tntMinecartMode
            item is ItemSkull -> skullMode
            item == Item.getItemFromBlock(Blocks.CONCRETE_POWDER) -> sandMode
            item == Item.getItemFromBlock(Blocks.ENDER_CHEST) -> enderChestMode
            else -> ItemMode.DISABLED
        }
    }

    private fun getPriorityFromItem(item: Item, zero: Boolean): Int {
        var result = when {
            item == Items.TOTEM_OF_UNDYING -> 1
            item is ItemBed -> 4
            item == Items.END_CRYSTAL -> 4
            item == Items.GOLDEN_APPLE -> 3
            item == Item.getItemFromBlock(Blocks.OBSIDIAN) -> 4
            item == Items.PEARL -> 4
            item is ItemPiston -> 4
            item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) -> 4
            item == Items.TNT_MINECART -> 4
            item == Item.getItemFromBlock(Blocks.RAIL) -> 4
            item is ItemSkull -> 0
            item == Item.getItemFromBlock(Blocks.CONCRETE_POWDER) -> 0
            item == Item.getItemFromBlock(Blocks.ENDER_CHEST) -> 0
            else -> 0
        }
        if (zero && result > 1) result--
        return result
    }

    private fun getThresholdFromItem(item: Item): Int {
        return when {
            item == Items.TOTEM_OF_UNDYING -> totemThreshold
            item is ItemBed -> bedThreshold
            item == Items.END_CRYSTAL -> crystalThreshold
            item == Items.GOLDEN_APPLE -> gappleThreshold
            item == Item.getItemFromBlock(Blocks.OBSIDIAN) -> obsidianThreshold
            item == Items.PEARL -> pearlThreshold
            item is ItemPiston -> pistonThreshold
            item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) -> powerThreshold
            item == Items.TNT_MINECART -> tntMinecartThreshold
            item == Item.getItemFromBlock(Blocks.RAIL) -> railThreshold
            item is ItemSkull -> skullThreshold
            item == Item.getItemFromBlock(Blocks.CONCRETE_POWDER) -> sandThreshold
            item == Item.getItemFromBlock(Blocks.ENDER_CHEST) -> enderChestThreshold
            else -> 0
        }
    }

    private fun getSoundFromItem(item: Item): SoundEvent {
        return when {
            item == Items.TOTEM_OF_UNDYING -> SoundEvents.BLOCK_ANVIL_LAND
            item is ItemBed -> SoundEvents.BLOCK_WOOD_BREAK
            item == Items.END_CRYSTAL -> SoundEvents.BLOCK_GLASS_BREAK
            item == Items.GOLDEN_APPLE -> SoundEvents.ENTITY_PLAYER_BURP
            item == Item.getItemFromBlock(Blocks.OBSIDIAN) -> SoundEvents.BLOCK_STONE_PLACE
            item == Items.PEARL -> SoundEvents.ENTITY_SHULKER_TELEPORT
            item is ItemPiston -> SoundEvents.BLOCK_PISTON_CONTRACT
            item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) -> SoundEvents.BLOCK_STONE_BREAK
            item == Items.TNT_MINECART -> SoundEvents.ENTITY_TNT_PRIMED
            item == Item.getItemFromBlock(Blocks.RAIL) -> SoundEvents.ENTITY_MINECART_RIDING
            item is ItemSkull -> SoundEvents.ENTITY_SKELETON_HURT
            item == Item.getItemFromBlock(Blocks.CONCRETE_POWDER) -> SoundEvents.BLOCK_SAND_PLACE
            item == Item.getItemFromBlock(Blocks.ENDER_CHEST) -> SoundEvents.BLOCK_ENDERCHEST_CLOSE
            else -> SoundEvents.BLOCK_STONE_STEP
        }
    }

    private fun isItemSoundEnabled(item: Item): Boolean {
        return when {
            item == Items.TOTEM_OF_UNDYING -> totemSound
            item is ItemBed -> bedSound
            item == Items.END_CRYSTAL -> crystalSound
            item == Items.GOLDEN_APPLE -> gappleSound
            item == Item.getItemFromBlock(Blocks.OBSIDIAN) -> obsidianSound
            item == Items.PEARL -> pearlSound
            item is ItemPiston -> pistonSound
            item == Item.getItemFromBlock(Blocks.REDSTONE_BLOCK) -> redstoneSound
            item == Items.TNT_MINECART -> tntMinecartSound
            item == Item.getItemFromBlock(Blocks.RAIL) -> railSound
            item is ItemSkull -> skullSound
            item == Item.getItemFromBlock(Blocks.CONCRETE_POWDER) -> sandSound
            item == Item.getItemFromBlock(Blocks.ENDER_CHEST) -> enderChestSound
            else -> false
        }
    }

    private fun SafeClientEvent.getItemCount(item: Item): Int {
        return DefinedKt.getAllSlotsPrioritized(player).filter { it.stack.item == item }.sumBy { it.stack.count }
    }

    private enum class ArmorValid(override val displayName: CharSequence) : DisplayEnum { ANY("Any Armor"), DIAMONDONLY("Diamond Only") }
    private data class ItemAlert(val item: Item, val count: Int, val threshold: Int, val sound: Boolean)
    private enum class ItemMode(override val displayName: CharSequence) : DisplayEnum { DISABLED("Disabled"), NETHERONLY("Nether Only"), NOTNETHER("Not Nether"), ALWAYS("Always") }
    private enum class Page(override val displayName: CharSequence) : DisplayEnum { GENERAL("General"), PRIORITY1("Priority 1"), PRIORITY2("Priority 2"), PRIORITY3("Priority 3") }
}
