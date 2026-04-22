package dev.wizard.meta.mixins.core.tileentity

import dev.wizard.meta.module.modules.render.DyeSpoofer
import net.minecraft.client.renderer.tileentity.TileEntityBedRenderer
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntityBed
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(TileEntityBedRenderer::class)
abstract class MixinTileEntityBedRenderer {
    @Redirect(method = ["render(Lnet/minecraft/tileentity/TileEntityBed;DDDFIF)V"], at = At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityBed;getColor()Lnet/minecraft/item/EnumDyeColor;"))
    private fun redirectGetColor(te: TileEntityBed): EnumDyeColor {
        if (DyeSpoofer.isEnabled && DyeSpoofer.bedColor != DyeSpoofer.DyeColor.DEFAULT) {
            return DyeSpoofer.bedBlockColor
        }
        return te.color
    }
}
