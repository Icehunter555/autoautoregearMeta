package dev.wizard.meta.mixins.accessor.entity

import net.minecraft.entity.item.EntityFireworkRocket
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(EntityFireworkRocket::class)
interface AccessorEntityFirework {
    @Accessor("fireworkAge")
    fun getFireworkAge(): Int

    @Accessor("fireworkAge")
    fun setFireworkAge(var1: Int)

    @Accessor("lifetime")
    fun getLifetime(): Int

    @Accessor("lifetime")
    fun setLifetime(var1: Int)
}
