package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.accessor.setRightClickDelayTimer
import net.minecraft.init.Items
import net.minecraft.item.*
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand
import kotlin.random.Random

object FastUse : Module(
    "FastUse",
    category = Category.PLAYER,
    description = "Use items faster"
) {
    var multiUse by setting(this, IntegerSetting(settingName("Multi Use"), 1, 1..10, 1))
    var delay by setting(this, IntegerSetting(settingName("Delay"), 0, 0..10, 1))
    private val blocks by setting(this, BooleanSetting(settingName("Blocks"), false))
    var allItems by setting(this, BooleanSetting(settingName("All Items"), true))
    var onlyPotions by setting(this, BooleanSetting(settingName("Only Potions"), false, { !allItems }))
    private val bow by setting(this, BooleanSetting(settingName("Bow"), true))
    private val chargeSetting by setting(this, IntegerSetting(settingName("Bow Charge"), 3, 0..20, 1, { allItems || bow }))
    private val chargeVariation by setting(this, IntegerSetting(settingName("Charge Variation"), 5, 0..20, 1, { allItems || bow }))

    private var lastUsedHand = EnumHand.MAIN_HAND
    private var randomVariation = 0

    @JvmStatic
    fun getBowCharge(): Double? {
        if (!isEnabled || (!allItems && !bow)) return null
        return 72000.0 - (chargeSetting.toDouble() + chargeVariation.toDouble() / 2.0)
    }

    private fun getInternalBowCharge(): Int {
        if (randomVariation == 0) {
            randomVariation = if (chargeVariation == 0) 0 else Random.nextInt(0, chargeVariation + 1)
        }
        return chargeSetting + randomVariation
    }

    private fun isPotionItem(item: Item): Boolean = item is ItemPotion || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION

    @JvmStatic
    fun shouldApplyFastUse(): Boolean {
        val player = mc.player ?: return false
        val item = player.getHeldItem(lastUsedHand).item
        return when {
            allItems -> item !is ItemAir && item !is ItemBlock
            onlyPotions -> isPotionItem(item)
            else -> false
        }
    }

    @JvmStatic
    fun updateRightClickDelay() {
        if (!isEnabled) return
        val player = mc.player ?: return
        val item = player.getHeldItem(lastUsedHand).item
        if (item is ItemAir) return

        if (allItems) {
            if (item !is ItemBlock) mc.setRightClickDelayTimer(delay)
        } else if (onlyPotions) {
            if (isPotionItem(item)) mc.setRightClickDelayTimer(delay)
            else mc.setRightClickDelayTimer(4)
        }
    }

    init {
        safeListener<TickEvent.Post> {
            if (player.isSpectator) return@safeListener

            if ((allItems || bow) && player.isHandActive && player.activeItemStack.item == Items.BOW) {
                if (player.itemInUseMaxCount >= getInternalBowCharge()) {
                    randomVariation = 0
                    mc.playerController.onStoppedUsingItem(player)
                }
            }
        }

        listener<PacketEvent.PostSend> {
            val packet = it.packet
            if (packet is CPacketPlayerTryUseItem) {
                lastUsedHand = packet.hand
            } else if (packet is CPacketPlayerTryUseItemOnBlock) {
                lastUsedHand = packet.hand
            }
        }
    }
}
