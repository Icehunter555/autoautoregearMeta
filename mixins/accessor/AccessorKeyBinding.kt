package dev.wizard.meta.mixins.accessor

import net.minecraft.client.settings.KeyBinding
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(KeyBinding::class)
interface AccessorKeyBinding {
    @Invoker("unpressKey")
    fun `troll$invoke$unpressKey`()

    @Accessor("pressTime")
    fun getPressTime(): Int

    @Accessor("pressTime")
    fun setPressTime(var1: Int)
}
