package dev.wizard.meta.util.inventory

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.accessor.attackDamage
import io.netty.buffer.Unpooled
import net.minecraft.block.Block
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Enchantments
import net.minecraft.item.*
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionUtils
import net.minecraft.util.EnumHand

val ItemStack.originalName: String
    get() = item.getItemStackDisplayName(this)

val Item.id: Int
    get() = Item.getIdFromItem(this)

val Item.block: Block
    get() = Block.getBlockFromItem(this)

val Item.isWeapon: Boolean
    get() = this is ItemSword || this is ItemAxe

val Item.isTool: Boolean
    get() = this is ItemTool || this is ItemSword || this is ItemHoe || this is ItemShears

val ItemFood.foodValue: Int
    get() = getHealAmount(ItemStack.EMPTY)

val ItemFood.saturation: Float
    get() = foodValue * getSaturationModifier(ItemStack.EMPTY) * 2.0f

val ItemStack.attackDamage: Float
    get() {
        val i = item
        val sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, this)
        val base = i.baseAttackDamage
        val modifier = if (sharpness > 0) sharpness * 0.5f + 0.5f else 0.0f
        return base + modifier
    }

val Item.baseAttackDamage: Float
    get() = when (this) {
        is ItemSword -> attackDamage + 4.0f
        is ItemTool -> attackDamage + 1.0f
        else -> 1.0f
    }

val Item.regName: String
    get() = registryName?.toString() ?: "minecraft:air"

val ItemStack.regName: String
    get() = item.regName

val ItemStack.durability: Int
    get() = maxDamage - itemDamage

val ItemStack.duraPercentage: Int
    get() = MathUtilKt.ceilToInt(durability * 100.0f / maxDamage)

fun ItemStack.getEnchantmentLevel(enchantment: Enchantment): Int {
    return EnchantmentHelper.getEnchantmentLevel(enchantment, this)
}

fun SafeClientEvent.itemPayload(item: ItemStack, channel: String) {
    val buffer = PacketBuffer(Unpooled.buffer())
    buffer.writeItemStack(item)
    connection.sendPacket(CPacketCustomPayload(channel, buffer))
}

fun EntityLivingBase.isHolding(hand: EnumHand, item: Item): Boolean {
    return getHeldItem(hand).item === item
}

fun EntityLivingBase.isHolding(hand: EnumHand, block: Block): Boolean {
    return getHeldItem(hand).item.block === block
}

fun ItemStack.hasPotion(potion: Potion): Boolean {
    return PotionUtils.getEffectsFromStack(this).any { it.potion === potion }
}
