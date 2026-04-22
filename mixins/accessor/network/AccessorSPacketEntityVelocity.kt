package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketEntityVelocity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketEntityVelocity::class)
interface AccessorSPacketEntityVelocity {
    @Accessor("motionX")
    fun setMotionX(var1: Int)

    @Accessor("motionY")
    fun setMotionY(var1: Int)

    @Accessor("motionZ")
    fun setMotionZ(var1: Int)
}
