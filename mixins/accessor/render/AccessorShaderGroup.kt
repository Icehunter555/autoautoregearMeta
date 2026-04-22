package dev.wizard.meta.mixins.accessor.render

import java.util.List
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.Shader
import net.minecraft.client.shader.ShaderGroup
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ShaderGroup::class)
interface AccessorShaderGroup {
    @Accessor("listShaders")
    fun getListShaders(): List<Shader>

    @Accessor("listFramebuffers")
    fun getListFramebuffers(): List<Framebuffer>
}
