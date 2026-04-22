package dev.wizard.meta.mixins.accessor.render

import java.util.Map
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.ShaderGroup
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(RenderGlobal::class)
interface AccessorRenderGlobal {
    @Accessor("entityOutlineShader")
    fun getEntityOutlineShader(): ShaderGroup

    @Accessor("damagedBlocks")
    fun trollGetDamagedBlocks(): Map<Int, DestroyBlockProgress>

    @Accessor("renderEntitiesStartupCounter")
    fun trollGetRenderEntitiesStartupCounter(): Int

    @Accessor("renderEntitiesStartupCounter")
    fun trollSetRenderEntitiesStartupCounter(var1: Int)

    @Accessor("countEntitiesTotal")
    fun trollGetCountEntitiesTotal(): Int

    @Accessor("countEntitiesTotal")
    fun trollSetCountEntitiesTotal(var1: Int)

    @Accessor("countEntitiesRendered")
    fun trollGetCountEntitiesRendered(): Int

    @Accessor("countEntitiesRendered")
    fun trollSetCountEntitiesRendered(var1: Int)

    @Accessor("countEntitiesHidden")
    fun trollGetCountEntitiesHidden(): Int

    @Accessor("countEntitiesHidden")
    fun trollSetCountEntitiesHidden(var1: Int)

    @Accessor("entityOutlineFramebuffer")
    fun trollGetEntityOutlineFramebuffer(): Framebuffer
}
