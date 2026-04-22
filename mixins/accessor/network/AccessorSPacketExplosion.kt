package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketExplosion
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketExplosion::class)
interface AccessorSPacketExplosion {
    @Accessor("motionX")
    fun setMotionX(var1: Float)

    @Accessor("motionY")
    fun setMotionY(var1: Float)

    @Accessor("motionZ")
    fun setMotionZ(var1: Float)
}
