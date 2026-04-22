package dev.wizard.meta.mixins.accessor.render

import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ItemRenderer::class)
interface AccessorItemRenderer {
    @Accessor("equippedProgressMainHand")
    fun setEquippedProgressMainHand(var1: Float)

    @Accessor("equippedProgressMainHand")
    fun getEquippedProgressMainHand(): Float

    @Accessor("equippedProgressOffHand")
    fun setEquippedProgressOffHand(var1: Float)

    @Accessor("equippedProgressOffHand")
    fun getEquippedProgressOffHand(): Float

    @Accessor("prevEquippedProgressMainHand")
    fun setPrevEquippedProgressMainHand(var1: Float)

    @Accessor("prevEquippedProgressMainHand")
    fun getPrevEquippedProgressMainHand(): Float

    @Accessor("prevEquippedProgressOffHand")
    fun getPrevEquippedProgressOffHand(): Float

    @Accessor("prevEquippedProgressOffHand")
    fun setPrevEquippedProgressOffHand(var1: Float)

    @Accessor("itemStackMainHand")
    fun setItemStackMainHand(var1: ItemStack)

    @Accessor("itemStackMainHand")
    fun getItemStackMainHand(): ItemStack

    @Accessor("itemStackOffHand")
    fun setItemStackOffHand(var1: ItemStack)

    @Accessor("itemStackOffHand")
    fun getItemStackOffHand(): ItemStack
}
