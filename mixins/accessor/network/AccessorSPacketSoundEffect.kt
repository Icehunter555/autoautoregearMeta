package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketSoundEffect
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketSoundEffect::class)
interface AccessorSPacketSoundEffect {
    @Accessor("soundPitch")
    fun setPitch(var1: Float)

    @Accessor("soundVolume")
    fun setVolume(var1: Float)
}
