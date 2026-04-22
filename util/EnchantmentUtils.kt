package dev.wizard.meta.util

import net.minecraft.enchantment.Enchantment
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagList
import kotlin.math.abs

object EnchantmentUtils {
    private val enchantmentMap: Map<Enchantment, EnumEnchantments> = EnumEnchantments.entries.associateBy { it.enchantment }

    fun getAllEnchantments(itemStack: ItemStack): List<LeveledEnchantment> {
        val enchantmentList = mutableListOf<LeveledEnchantment>()
        val nbtTagList: NBTTagList? = if (itemStack.item === Items.field_151134_bR) {
            itemStack.tagCompound?.getTagList("StoredEnchantments", 10)
        } else {
            itemStack.enchantmentTagList
        }

        if (nbtTagList == null) return enchantmentList

        for (i in 0 until nbtTagList.tagCount()) {
            val compound = nbtTagList.getCompoundTagAt(i)
            val enchantment = Enchantment.getEnchantmentByID(compound.getShort("id").toInt()) ?: continue
            val level = compound.getShort("lvl")
            enchantmentList.add(LeveledEnchantment(enchantment, level))
        }
        return enchantmentList
    }

    fun getEnchantmentAlias(enchantment: Enchantment): String {
        return getEnumEnchantment(enchantment)?.alias ?: "Null"
    }

    fun getEnumEnchantment(enchantment: Enchantment): EnumEnchantments? {
        return enchantmentMap[enchantment]
    }

    enum class EnumEnchantments(val enchantment: Enchantment, val alias: String) {
        PROTECTION(Enchantments.field_180310_c, "PRO"),
        FIRE_PROTECTION(Enchantments.field_77329_d, "FRP"),
        FEATHER_FALLING(Enchantments.field_180309_e, "FEA"),
        BLAST_PROTECTION(Enchantments.field_185297_d, "BLA"),
        PROJECTILE_PROTECTION(Enchantments.field_180308_g, "PJP"),
        RESPIRATION(Enchantments.field_185298_f, "RES"),
        AQUA_AFFINITY(Enchantments.field_185299_g, "AQU"),
        THORNS(Enchantments.field_92091_k, "THR"),
        DEPTH_STRIDER(Enchantments.field_185300_i, "DEP"),
        FROST_WALKER(Enchantments.field_185301_j, "FRO"),
        BINDING_CURSE(Enchantments.field_190941_k, "BIN"),
        SHARPNESS(Enchantments.field_185302_k, "SHA"),
        SMITE(Enchantments.field_185303_l, "SMI"),
        BANE_OF_ARTHROPODS(Enchantments.field_180312_n, "BAN"),
        KNOCKBACK(Enchantments.field_180313_o, "KNB"),
        FIRE_ASPECT(Enchantments.field_77334_n, "FIA"),
        LOOTING(Enchantments.field_185304_p, "LOO"),
        SWEEPING(Enchantments.field_191530_r, "SWE"),
        EFFICIENCY(Enchantments.field_185305_q, "EFF"),
        SILK_TOUCH(Enchantments.field_185306_r, "SIL"),
        UNBREAKING(Enchantments.field_185307_s, "UNB"),
        FORTUNE(Enchantments.field_185308_t, "FOT"),
        POWER(Enchantments.field_185309_u, "POW"),
        PUNCH(Enchantments.field_185310_v, "PUN"),
        FLAME(Enchantments.field_185311_w, "FLA"),
        INFINITY(Enchantments.field_185312_x, "INF"),
        LUCK_OF_THE_SEA(Enchantments.field_151370_z, "LUC"),
        LURE(Enchantments.field_151369_A, "LUR"),
        MENDING(Enchantments.field_185296_A, "MEN"),
        VANISHING_CURSE(Enchantments.field_190940_C, "VAN")
    }

    class LeveledEnchantment(val enchantment: Enchantment, val level: Short) {
        val isSingleLevel: Boolean = enchantment.maxLevel == 1
        val isMax: Boolean = level >= enchantment.maxLevel
        val is32K: Boolean = abs(level.toInt()) >= 32000
        val alias: String = getEnchantmentAlias(enchantment)
        val levelText: String = if (isSingleLevel) "" else if (is32K) "32K" else if (isMax) "MAX" else level.toString()
    }
}
