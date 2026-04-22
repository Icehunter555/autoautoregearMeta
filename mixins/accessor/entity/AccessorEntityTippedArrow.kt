package dev.wizard.meta.mixins.accessor.entity

import net.minecraft.entity.projectile.EntityTippedArrow
import net.minecraft.potion.PotionType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(EntityTippedArrow::class)
interface AccessorEntityTippedArrow {
    @Accessor("potion")
    fun getPotion(): PotionType
}
