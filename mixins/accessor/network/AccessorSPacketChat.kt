package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketChat::class)
interface AccessorSPacketChat {
    @Accessor("chatComponent")
    fun setChatComponent(var1: ITextComponent)
}
