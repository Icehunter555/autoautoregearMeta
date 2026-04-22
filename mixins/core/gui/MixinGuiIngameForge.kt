package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.player.Freecam
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.GuiIngameForge
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(GuiIngameForge::class, remap = false)
class MixinGuiIngameForge {
    @ModifyVariable(method = ["renderAir"], at = At(value = "STORE", ordinal = 0))
    private fun `renderAir$getRenderViewEntity`(renderViewEntity: EntityPlayer): EntityPlayer {
        return Freecam.getRenderViewEntity(renderViewEntity)
    }

    @ModifyVariable(method = ["renderHealth"], at = At(value = "STORE", ordinal = 0))
    private fun `renderHealth$getRenderViewEntity`(renderViewEntity: EntityPlayer): EntityPlayer {
        return Freecam.getRenderViewEntity(renderViewEntity)
    }

    @ModifyVariable(method = ["renderFood"], at = At(value = "STORE", ordinal = 0))
    private fun `renderFood$getRenderViewEntity`(renderViewEntity: EntityPlayer): EntityPlayer {
        return Freecam.getRenderViewEntity(renderViewEntity)
    }

    @ModifyVariable(method = ["renderHealthMount"], at = At(value = "STORE", ordinal = 0))
    private fun `renderHealthMount$getRenderViewEntity`(renderViewEntity: EntityPlayer): EntityPlayer {
        return Freecam.getRenderViewEntity(renderViewEntity)
    }
}
