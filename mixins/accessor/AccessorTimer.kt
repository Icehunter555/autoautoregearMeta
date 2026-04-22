package dev.wizard.meta.mixins.accessor

import net.minecraft.util.Timer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(Timer::class)
interface AccessorTimer {
    @Accessor("tickLength")
    fun trollGetTickLength(): Float

    @Accessor("tickLength")
    fun trollSetTickLength(var1: Float)
}
