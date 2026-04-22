package dev.wizard.meta.mixins.accessor.network

import net.minecraft.network.play.server.SPacketEntityStatus
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(SPacketEntityStatus::class)
interface AccessorSPacketEntityStatus {
    @Accessor("entityId")
    fun trollGetEntityID(): Int
}
