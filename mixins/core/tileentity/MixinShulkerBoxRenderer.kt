package dev.wizard.meta.mixins.core.tileentity

import dev.wizard.meta.module.modules.render.DyeSpoofer
import net.minecraft.client.renderer.tileentity.TileEntityShulkerBoxRenderer
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntityShulkerBox
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(TileEntityShulkerBoxRenderer::class)
abstract class MixinShulkerBoxRenderer {
    @Redirect(method = ["render(Lnet/minecraft/tileentity/TileEntityShulkerBox;DDDFIF)V"], at = At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntityShulkerBox;getColor()Lnet/minecraft/item/EnumDyeColor;"))
    private fun redirectGetColor(te: TileEntityShulkerBox): EnumDyeColor {
        if (DyeSpoofer.isEnabled && DyeSpoofer.shulkerColor != DyeSpoofer.DyeColor.DEFAULT) {
            return DyeSpoofer.shulkerBlockColor
        }
        return te.color
    }
}
