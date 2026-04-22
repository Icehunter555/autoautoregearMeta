package dev.wizard.meta.gui.hudgui.elements.hud

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.font.TextComponent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.delegate.FrameFloat
import dev.wizard.meta.util.inventory.block
import dev.wizard.meta.util.inventory.hasPotion
import dev.wizard.meta.util.inventory.ItemStackPredicate
import dev.wizard.meta.util.inventory.slot.allSlots
import net.minecraft.block.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.init.PotionTypes
import net.minecraft.item.Item
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionUtils

object CombatItemCount : LabelHud("Combat Item Count", category = Category.HUD, description = "Counts combat items like gapples, crystal, etc") {

    private val itemSettings = arrayOf(
        ItemSetting("Bed", ItemStackPredicate.byItem(Items.BED)),
        ItemSetting("Crystal", ItemStackPredicate.byItem(Items.END_CRYSTAL)),
        ItemSetting("Gapple", ItemStackPredicate.byItem(Items.GOLDEN_APPLE)),
        ItemSetting("Totem", ItemStackPredicate.byItem(Items.TOTEM_OF_UNDYING)),
        ItemSetting("Pearl", ItemStackPredicate.byItem(Items.ENDER_PEARL)),
        ItemSetting("Skull", { it.item is ItemSkull }),
        ItemSetting("Sand", { val b = it.item.block; b is BlockFalling && b !is BlockAnvil && b !is BlockGravel }),
        ItemSetting("Obsidian", ItemStackPredicate.byItem(Item.getItemFromBlock(Blocks.OBSIDIAN))),
        ItemSetting("Chorus Fruit", ItemStackPredicate.byItem(Items.CHORUS_FRUIT)),
        ItemSetting("Ender Chest", ItemStackPredicate.byItem(Item.getItemFromBlock(Blocks.ENDER_CHEST))),
        ItemSetting("Regears", { it.item.block is BlockShulkerBox }),
        ItemSetting("Health Potions", { it.item == Items.SPLASH_POTION && it.hasPotion(MobEffects.INSTANT_HEALTH) }),
        ItemSetting("Speed Potions", { it.item == Items.SPLASH_POTION && it.hasPotion(MobEffects.SPEED) }),
        ItemSetting("Tnt Minecarts", ItemStackPredicate.byItem(Items.TNT_MINECART)),
        ItemSetting("Rails", { isRail(it) })
    )

    private val hideMissingItems by setting(this, "Hide Missing Items", false)
    private val showIcon by setting(this, "Show Icon", true)
    private val horizontal by setting(this, "Horizontal", true, visibility = { showIcon })

    private val itemStacks = arrayOf(
        ItemStack(Items.BED, -1),
        ItemStack(Items.END_CRYSTAL, -1),
        ItemStack(Items.GOLDEN_APPLE, -1, 1),
        ItemStack(Items.TOTEM_OF_UNDYING, -1),
        ItemStack(Items.ENDER_PEARL, -1),
        ItemStack(Items.SKULL, -1),
        ItemStack(Item.getItemFromBlock(Blocks.SAND)),
        ItemStack(Item.getItemFromBlock(Blocks.OBSIDIAN)),
        ItemStack(Items.CHORUS_FRUIT, -1),
        ItemStack(Item.getItemFromBlock(Blocks.ENDER_CHEST), -1),
        ItemStack(Item.getItemFromBlock(Blocks.WHITE_SHULKER_BOX), -1),
        PotionUtils.addPotionToItemStack(ItemStack(Items.SPLASH_POTION, -1), PotionTypes.HEALING),
        PotionUtils.addPotionToItemStack(ItemStack(Items.SPLASH_POTION, -1), PotionTypes.SWIFTNESS),
        ItemStack(Items.TNT_MINECART, -1),
        ItemStack(Item.getItemFromBlock(Blocks.RAIL), -1)
    )

    override val hudWidth by FrameFloat {
        if (showIcon) {
            val visibleCount = visibleItems.size
            if (horizontal) 20.0f * visibleCount else 20.0f
        } else {
            displayText.width
        }
    }

    override val hudHeight by FrameFloat {
        if (showIcon) {
            val visibleCount = visibleItems.size
            if (horizontal) 20.0f else 20.0f * visibleCount
        } else {
            displayText.height
        }
    }

    override fun updateText(event: SafeClientEvent) {
        val slots = event.player.allSlots
        displayText.clear()
        itemSettings.forEachIndexed { index, entry ->
            val count = if (entry.isEnabled()) {
                slots.asSequence().filter { entry.predicate(it.stack) }.sumOf { it.stack.count }
            } else {
                -1
            }

            if (showIcon) {
                itemStacks[index].count = count + 1
            } else if (count > -1 && (!hideMissingItems || count > 0)) {
                displayText.add(entry.name, ClickGUI.text)
                displayText.addLine("x$count", ClickGUI.primary)
            }
        }
    }

    private val visibleItems: List<ItemStack>
        get() = itemStacks.asSequence()
            .filter { !hideMissingItems || it.count > 0 }
            .sortedByDescending { it.count }
            .toList()

    override fun renderHud() {
        if (showIcon) {
            GlStateManager.pushMatrix()
            visibleItems.forEach {
                RenderUtils2D.drawItem(it, 2, 2, (it.count - 1).toString())
                if (horizontal) {
                    GlStateManager.translate(20.0f, 0.0f, 0.0f)
                } else {
                    GlStateManager.translate(0.0f, 20.0f, 0.0f)
                }
            }
            GlStateManager.popMatrix()
        } else {
            super.renderHud()
        }
    }

    private fun isRail(stack: ItemStack): Boolean {
        val i = stack.item
        return i == Item.getItemFromBlock(Blocks.RAIL) || i == Item.getItemFromBlock(Blocks.GOLDEN_RAIL) || i == Item.getItemFromBlock(Blocks.DETECTOR_RAIL) || i == Item.getItemFromBlock(Blocks.ACTIVATOR_RAIL)
    }

    private class ItemSetting(
        val name: String,
        val predicate: ItemStackPredicate,
        val enabled: (SafeClientEvent.() -> Boolean)? = null
    ) {
        private val setting = CombatItemCount.setting(this@CombatItemCount, name, true)

        fun isEnabled(): Boolean {
            return SafeClientEvent.instance?.let {
                setting.value && (enabled == null || enabled.invoke(it))
            } ?: false
        }
    }
}
