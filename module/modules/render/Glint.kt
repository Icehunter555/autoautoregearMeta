package dev.wizard.meta.module.modules.render

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.init.Items
import net.minecraft.item.Item
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object Glint : Module(
    "Glint",
    category = Category.RENDER,
    description = "Enhanced tooltips for various items"
) {
    val glintColor by setting(this, ColorSetting(settingName("Glint Color"), ColorRGB(255, 255, 255, 140)))
    val enchantTotems by setting(this, BooleanSetting(settingName("Totems Enchanted"), false))
    val enchantCrystals by setting(this, BooleanSetting(settingName("Crystals Enchanted"), true))
    val enchantChorus by setting(this, BooleanSetting(settingName("Chorus Enchanted"), false))
    val enchantPearls by setting(this, BooleanSetting(settingName("Pearls Enchanted"), false))
    val enchantPots by setting(this, BooleanSetting(settingName("Potions Enchanted"), true))
    val enchantGaps by setting(this, BooleanSetting(settingName("Gapples Enchanted"), true))
    val enchantCarts by setting(this, BooleanSetting(settingName("Carts Enchanted"), false))

    @JvmStatic
    fun getColor(): Int {
        val color = glintColor
        return (color.a shl 24) or (color.r shl 16) or (color.g shl 8) or color.b
    }

    @JvmStatic
    fun handleItemGlint(item: Item, cir: CallbackInfoReturnable<Boolean>) {
        if (!INSTANCE.isEnabled) return

        when (item) {
            Items.TOTEM_OF_UNDYING -> if (INSTANCE.enchantTotems) cir.returnValue = true
            Items.END_CRYSTAL -> if (!INSTANCE.enchantCrystals) cir.returnValue = false
            Items.CHORUS_FRUIT -> if (INSTANCE.enchantChorus) cir.returnValue = true
            Items.ENDER_PEARL -> if (INSTANCE.enchantPearls) cir.returnValue = true
            Items.POTIONITEM, Items.SPLASH_POTION, Items.LINGERING_POTION -> if (!INSTANCE.enchantPots) cir.returnValue = false
            Items.GOLDEN_APPLE -> if (!INSTANCE.enchantGaps) cir.returnValue = false
            Items.TNT_MINECART -> if (INSTANCE.enchantCarts) cir.returnValue = true
        }
    }
}
