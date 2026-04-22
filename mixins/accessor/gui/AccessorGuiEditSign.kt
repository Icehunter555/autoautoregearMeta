package dev.wizard.meta.mixins.accessor.gui

import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.tileentity.TileEntitySign
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(GuiEditSign::class)
interface AccessorGuiEditSign {
    @Accessor("tileSign")
    fun getTileSign(): TileEntitySign

    @Accessor("editLine")
    fun getEditLine(): Int
}
