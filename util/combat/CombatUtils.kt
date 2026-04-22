package dev.wizard.meta.util.combat

import dev.wizard.meta.event.AlwaysListening
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.parallelListener
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.util.inventory.getAttackDamage
import dev.wizard.meta.util.inventory.operation.swapToSlot
import dev.wizard.meta.util.inventory.slot.getHotbarSlots
import dev.wizard.meta.util.threads.runSafeSuspend
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.util.CombatRules
import net.minecraft.util.DamageSource
import java.util.*
import kotlin.math.max
import kotlin.math.rint

object CombatUtils : AlwaysListening {
    private val cachedArmorValues = WeakHashMap<EntityLivingBase, ArmorInfo>()

    fun SafeClientEvent.calcDamageFromPlayer(entity: EntityPlayer, assumeCritical: Boolean = false): Float {
        val itemStack = entity.heldItemMainhand
        var damage = itemStack.getAttackDamage()
        if (assumeCritical) damage *= 1.5f
        return calcDamage(player, damage)
    }

    fun SafeClientEvent.calcDamageFromMob(entity: EntityMob): Float {
        var damage = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).attributeValue.toFloat()
        damage += EnchantmentHelper.getModifierForCreature(entity.heldItemMainhand, entity.creatureAttribute)
        return calcDamage(player, damage)
    }

    fun calcDamage(entity: EntityLivingBase, damageIn: Float = 100f, source: DamageSource = DamageSource.GENERIC, roundDamage: Boolean = false): Float {
        val armorInfo = cachedArmorValues[entity] ?: return 0.0f
        var damage = CombatRules.getDamageAfterAbsorb(damageIn, armorInfo.armorValue, armorInfo.toughness)
        if (source != DamageSource.OUT_OF_WORLD) {
            entity.getActivePotionEffect(MobEffects.RESISTANCE)?.let {
                damage *= max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
            }
        }
        return if (roundDamage) {
            rint(damage.toDouble()).toFloat()
        } else {
            damage * getProtectionModifier(entity, source)
        }
    }

    private fun getProtectionModifier(entity: EntityLivingBase, damageSource: DamageSource): Float {
        var modifier = 0
        for (armor in entity.armorInventoryList) {
            if (armor.isEmpty) continue
            val nbtTagList = armor.enchantmentTagList
            for (i in 0 until nbtTagList.tagCount()) {
                val compoundTag = nbtTagList.getCompoundTagAt(i)
                val id = compoundTag.getInteger("id")
                val level = compoundTag.getInteger("lvl")
                Enchantment.getEnchantmentByID(id)?.let {
                    modifier += it.calcModifierDamage(level, damageSource)
                }
            }
        }
        modifier = modifier.coerceIn(0, 20)
        return 1.0f - modifier / 25.0f
    }

    fun SafeClientEvent.equipBestWeapon(preferWeapon: PreferWeapon = PreferWeapon.NONE, allowTool: Boolean = false) {
        val bestSlot = getHotbarSlots().filter {
            val item = it.stack.item
            item is ItemSword || item is ItemAxe || (allowTool && item is ItemTool)
        }.maxByOrNull {
            val stack = it.stack
            val damage = stack.getAttackDamage()
            when {
                preferWeapon == PreferWeapon.SWORD && stack.item is ItemSword -> damage * 10.0f
                preferWeapon == PreferWeapon.AXE && stack.item is ItemAxe -> damage * 10.0f
                else -> damage
            }
        }
        bestSlot?.let { swapToSlot(it) }
    }

    fun EntityLivingBase.getScaledHealth(): Float {
        return health + absorptionAmount * (health / maxHealth)
    }

    fun EntityLivingBase.getTotalHealth(): Float {
        return health + absorptionAmount
    }

    init {
        parallelListener<TickEvent.Post> {
            runSafeSuspend {
                for (entity in EntityManager.entity) {
                    if (entity is EntityLivingBase) {
                        val armorValue = entity.totalArmorValue.toFloat()
                        val toughness = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue.toFloat()
                        cachedArmorValues.getOrPut(entity) { ArmorInfo() }.update(armorValue, toughness)
                    }
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            cachedArmorValues.clear()
        }
    }

    private class ArmorInfo {
        var armorValue: Float = 0f
            private set
        var toughness: Float = 0f
            private set

        fun update(armorValue: Float, toughness: Float) {
            this.armorValue = armorValue
            this.toughness = toughness
        }
    }

    enum class PreferWeapon {
        SWORD, AXE, NONE
    }
}
