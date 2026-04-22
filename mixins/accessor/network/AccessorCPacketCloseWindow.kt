package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.client.CPacketCloseWindow
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(CPacketCloseWindow::class)
interface AccessorCPacketCloseWindow {
    @Accessor("windowId")
    fun trollGetWindowID(): Int
}
