package dev.wizard.meta.mixins.accessor.gui

import net.minecraft.client.gui.GuiDisconnected
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(GuiDisconnected::class)
interface AccessorGuiDisconnected {
    @Accessor("parentScreen")
    fun getParentScreen(): GuiScreen

    @Accessor("reason")
    fun getReason(): String

    @Accessor("message")
    fun getMessage(): ITextComponent
}
