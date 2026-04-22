package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.client.CPacketChatMessage
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(CPacketChatMessage::class)
interface AccessorCPacketChatMessage {
    @Accessor("message")
    fun setMessage(var1: String)
}
