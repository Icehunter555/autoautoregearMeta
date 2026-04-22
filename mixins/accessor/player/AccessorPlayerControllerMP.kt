package dev.wizard.meta.mixins.accessor.player

import net.minecraft.client.multiplayer.PlayerControllerMP
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(PlayerControllerMP::class)
interface AccessorPlayerControllerMP {
    @Accessor("blockHitDelay")
    fun getBlockHitDelay(): Int

    @Accessor("blockHitDelay")
    fun setBlockHitDelay(var1: Int)

    @Accessor("isHittingBlock")
    fun trollSetIsHittingBlock(var1: Boolean)

    @Accessor("currentPlayerItem")
    fun getCurrentPlayerItem(): Int

    @Invoker("syncCurrentPlayItem")
    fun trollInvokeSyncCurrentPlayItem()
}
