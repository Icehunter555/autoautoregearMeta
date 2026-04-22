package dev.wizard.meta.mixins.accessor

import net.minecraft.item.ItemTool
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ItemTool::class)
interface AccessorItemTool {
    @Accessor("attackDamage")
    fun getAttackDamage(): Float
}
