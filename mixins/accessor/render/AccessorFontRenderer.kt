package dev.wizard.meta.mixins.accessor.render

import net.minecraft.client.gui.FontRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(FontRenderer::class)
interface AccessorFontRenderer {
    @Invoker("renderChar")
    fun invokeRenderChar(var1: Char, var2: Boolean): Float

    @Invoker("resetStyles")
    fun invokeResetStyles()
}
