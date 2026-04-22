package dev.wizard.meta.mixins.accessor.gui

import net.minecraft.client.gui.GuiChat
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(GuiChat::class)
interface AccessorGuiChat {
    @Accessor("historyBuffer")
    fun getHistoryBuffer(): String

    @Accessor("historyBuffer")
    fun setHistoryBuffer(var1: String)

    @Accessor("sentHistoryCursor")
    fun getSentHistoryCursor(): Int

    @Accessor("sentHistoryCursor")
    fun setSentHistoryCursor(var1: Int)
}
