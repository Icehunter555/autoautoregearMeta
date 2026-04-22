package dev.wizard.meta.mixins.accessor

import net.minecraft.client.Minecraft
import net.minecraft.util.Timer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(Minecraft::class)
interface AccessorMinecraft {
    @Accessor("timer")
    fun trollGetTimer(): Timer

    @Accessor("renderPartialTicksPaused")
    fun getRenderPartialTicksPaused(): Float

    @Accessor("rightClickDelayTimer")
    fun getRightClickDelayTimer(): Int

    @Accessor("rightClickDelayTimer")
    fun setRightClickDelayTimer(var1: Int)

    @Invoker("rightClickMouse")
    fun invokeRightClickMouse()

    @Invoker("sendClickBlockToController")
    fun invokeSendClickBlockToController(var1: Boolean)
}
