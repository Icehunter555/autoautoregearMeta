package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(CPacketPlayerTryUseItemOnBlock::class)
interface AccessorCPacketPlayerTryUseItemOnBlock {
    @Accessor("placedBlockDirection")
    fun trollSetSide(var1: EnumFacing)
}
