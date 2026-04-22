package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.client.CPacketPlayer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(CPacketPlayer::class)
interface AccessorCPacketPlayer {
    @Accessor("x")
    fun setX(var1: Double)

    @Accessor("y")
    fun setY(var1: Double)

    @Accessor("z")
    fun setZ(var1: Double)

    @Accessor("yaw")
    fun setYaw(var1: Float)

    @Accessor("pitch")
    fun setPitch(var1: Float)

    @Accessor("onGround")
    fun setOnGround(var1: Boolean)

    @Accessor("moving")
    fun getMoving(): Boolean

    @Accessor("rotating")
    fun getRotating(): Boolean
}
