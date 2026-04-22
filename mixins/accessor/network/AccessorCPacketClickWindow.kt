package dev.wizard.meta.mixins.accessor.network

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketClickWindow
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(CPacketClickWindow::class)
interface AccessorCPacketClickWindow {
    @Accessor("clickedItem")
    fun trollSetClickedItem(var1: ItemStack)
}
