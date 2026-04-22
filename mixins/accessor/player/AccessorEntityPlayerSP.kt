package dev.wizard.meta.mixins.accessor.player

import net.minecraft.client.entity.EntityPlayerSP
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(EntityPlayerSP::class)
interface AccessorEntityPlayerSP {
    @Accessor("handActive")
    fun trollSetHandActive(var1: Boolean)
}
