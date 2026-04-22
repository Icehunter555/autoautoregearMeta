package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.util.Wrapper
import net.minecraft.client.renderer.debug.DebugRendererChunkBorder
import net.minecraft.entity.player.EntityPlayer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(DebugRendererChunkBorder::class)
class MixinDebugRendererChunkBorder {
    @ModifyVariable(method = ["render"], at = At(value = "STORE", ordinal = 0))
    fun render(entityPlayer: EntityPlayer): EntityPlayer {
        if (Wrapper.getMinecraft().renderViewEntity is EntityPlayer) {
            return Wrapper.getMinecraft().renderViewEntity as EntityPlayer
        }
        return Wrapper.getMinecraft().player
    }
}
