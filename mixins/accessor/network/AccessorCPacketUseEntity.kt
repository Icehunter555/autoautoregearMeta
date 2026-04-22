package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.client.CPacketUseEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(CPacketUseEntity::class)
interface AccessorCPacketUseEntity {
    @Accessor("entityId")
    fun getId(): Int

    @Accessor("entityId")
    fun setId(var1: Int)

    @Accessor("action")
    fun setAction(var1: CPacketUseEntity.Action)
}
