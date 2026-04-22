package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.NoRender
import dev.wizard.meta.util.accessor.GuiKt
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer
import net.minecraft.tileentity.TileEntitySign
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(TileEntitySignRenderer::class)
class MixinTileEntitySignRenderer {
    private val mc = Minecraft.getMinecraft()

    @Redirect(method = ["render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V"], at = At(value = "FIELD", target = "Lnet/minecraft/tileentity/TileEntitySign;signText:[Lnet/minecraft/util/text/ITextComponent;", opcode = 180))
    fun getRenderViewEntity(sign: TileEntitySign): Array<ITextComponent> {
        if (NoRender.isEnabled && NoRender.signText) {
            val screen = this.mc.currentScreen
            if (screen is GuiEditSign && GuiKt.getTileSign(screen) == sign) {
                return sign.signText
            }
            return emptyArray()
        }
        return sign.signText
    }
}
