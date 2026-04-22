package dev.wizard.meta.mixins.accessor.render

import net.minecraft.client.renderer.entity.RenderManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(RenderManager::class)
interface AccessorRenderManager {
    @Accessor("renderPosX")
    fun getRenderPosX(): Double

    @Accessor("renderPosY")
    fun getRenderPosY(): Double

    @Accessor("renderPosZ")
    fun getRenderPosZ(): Double

    @Accessor("renderOutlines")
    fun getRenderOutlines(): Boolean
}
