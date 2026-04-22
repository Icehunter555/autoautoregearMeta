package dev.wizard.meta.mixins.accessor

import net.minecraft.inventory.Slot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(Slot::class)
interface AccessorSlot {
    @Invoker("onSwapCraft")
    fun trollOnSwapCraft(var1: Int)
}
