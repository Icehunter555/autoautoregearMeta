package dev.wizard.meta.mixins.accessor.render

import net.minecraft.client.renderer.DestroyBlockProgress
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(DestroyBlockProgress::class)
interface AccessorDestroyBlockProgress {
    @Accessor("miningPlayerEntId")
    fun trollGetEntityID(): Int
}
