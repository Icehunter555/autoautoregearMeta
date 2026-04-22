package dev.wizard.meta.mixins.accessor.render

import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(Render::class)
interface AccessorRender {
    @Accessor
    fun getRenderOutlines(): Boolean

    @Invoker
    fun callGetTeamColor(var1: Entity): Int
}
