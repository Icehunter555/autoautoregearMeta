package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.process.PauseProcess
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.combat.CombatUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.inventory.slot.firstByStack
import dev.wizard.meta.util.inventory.slot.hotbarSlots
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand

object AutoEat : Module(
    "AutoEat",
    category = Category.PLAYER,
    description = "Automatically eat"
) {
    private val mode by setting(this, EnumSetting(settingName("Mode"), Mode.SMART))
    private val belowHunger by setting(this, IntegerSetting(settingName("Below Hunger"), 15, 1..20, 1, { mode != Mode.ALWAYS }))
    private val belowHealth by setting(this, IntegerSetting(settingName("Below Health"), 10, 1..36, 1, { mode != Mode.ALWAYS }))
    private val onlyDamage by setting(this, BooleanSetting(settingName("Only eat if taking damage"), false, { mode != Mode.ALWAYS }))
    private val onlyGapple by setting(this, BooleanSetting(settingName("Only Gapple"), true))
    private val pauseBaritone by setting(this, BooleanSetting(settingName("Pause Baritone"), true, { mode != Mode.ALWAYS }))
    private val autoSwitch by setting(this, BooleanSetting(settingName("Auto Switch"), true))
    private val ignoreSwitch by setting(this, BooleanSetting(settingName("Ignore Switch"), false))

    private var lastSlot = -1
    private var eating = false
    private var lastHealth = 0.0f

    override fun getHudInfo(): String = if (eating) "Eating" else "Idle"
    override fun isActive(): Boolean = isEnabled && eating

    init {
        onDisable {
            stopEating()
            swapBack()
        }

        safeListener<TickEvent.Pre> {
            if (!player.isEntityAlive) {
                if (eating) stopEating()
                return@safeListener
            }

            if (ignoreSwitch && lastSlot != -1 && player.inventory.currentItem != lastSlot) {
                lastHealth = player.health
                return@safeListener
            }

            val hand = if (shouldEat(this)) {
                if (isValid(this, player.heldItemOffhand)) {
                    EnumHand.OFF_HAND
                } else if (isValid(this, player.heldItemMainhand)) {
                    EnumHand.MAIN_HAND
                } else if (autoSwitch && swapToFood(this)) {
                    startEating()
                    return@safeListener
                } else {
                    null
                }
            } else {
                null
            }

            if (hand != null) {
                eat(this, hand)
            } else if (eating) {
                stopEating()
            } else if (autoSwitch) {
                swapBack()
            }

            lastHealth = player.health
        }
    }

    private fun shouldEat(event: SafeClientEvent): Boolean {
        if (mode == Mode.ALWAYS) return true
        if (onlyDamage) {
            return lastHealth.toInt() > event.player.health.toInt() && CombatUtils.getScaledHealth(event.player as EntityLivingBase) < belowHealth.toFloat()
        }
        return event.player.foodStats.foodLevel < belowHunger || CombatUtils.getScaledHealth(event.player as EntityLivingBase) < belowHealth.toFloat()
    }

    private fun eat(event: SafeClientEvent, hand: EnumHand) {
        if (!eating || !event.player.isHandActive || event.player.activeHand != hand) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            mc.playerController.processRightClick(event.player, event.world, hand)
        }
        startEating()
    }

    private fun startEating() {
        if (pauseBaritone) PauseProcess.pauseBaritone(this)
        eating = true
    }

    private fun stopEating() {
        PauseProcess.unpauseBaritone(this)
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
        eating = false
    }

    private fun swapBack() {
        if (lastSlot != -1) {
            val slot = lastSlot
            lastSlot = -1
            SafeClientEvent.instance?.swapToSlot(slot)
        }
    }

    private fun swapToFood(event: SafeClientEvent): Boolean {
        lastSlot = event.player.inventory.currentItem
        val foodSlot = event.player.hotbarSlots.firstByStack { isValid(event, it) }
        return if (foodSlot != null) {
            event.swapToSlot(foodSlot)
            true
        } else {
            false
        }
    }

    private fun isValid(event: SafeClientEvent, stack: ItemStack): Boolean {
        val item = stack.item
        if (item !is ItemFood || item == Items.CHORUS_FRUIT) return false
        if (isBadFood(stack, item)) return false
        if (onlyGapple && item != Items.GOLDEN_APPLE) return false
        return event.player.canEat(item == Items.GOLDEN_APPLE)
    }

    private fun isBadFood(stack: ItemStack, item: ItemFood): Boolean {
        return item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE || item == Items.POISONOUS_POTATO ||
                (item == Items.FISH && (stack.metadata == 3 || stack.metadata == 2))
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        ALWAYS("Always"), SMART("Smart")
    }
}
