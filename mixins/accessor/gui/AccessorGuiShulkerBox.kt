package dev.wizard.meta.mixins.accessor.gui

import net.minecraft.client.gui.inventory.GuiShulkerBox
import net.minecraft.inventory.IInventory
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(GuiShulkerBox::class)
interface AccessorGuiShulkerBox {
    @Accessor("inventory")
    fun getInventory(): IInventory
}
