package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketPlayerPosLook
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketPlayerPosLook::class)
interface AccessorSPacketPosLook {
    @Accessor("yaw")
    fun setYaw(var1: Float)

    @Accessor("pitch")
    fun setPitch(var1: Float)
}
