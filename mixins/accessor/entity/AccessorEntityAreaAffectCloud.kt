package dev.wizard.meta.mixins.accessor.entity

import net.minecraft.entity.EntityAreaEffectCloud
import net.minecraft.potion.PotionType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(EntityAreaEffectCloud::class)
interface AccessorEntityAreaAffectCloud {
    @Accessor("potion")
    fun getPotion(): PotionType
}
