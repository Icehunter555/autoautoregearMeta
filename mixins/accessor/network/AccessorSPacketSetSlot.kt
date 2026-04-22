package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketSetSlot
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketSetSlot::class)
interface AccessorSPacketSetSlot {
    @Accessor("windowId")
    fun trollSetWindowId(var1: Int)

    @Accessor("slot")
    fun trollSetSlot(var1: Int)
}
