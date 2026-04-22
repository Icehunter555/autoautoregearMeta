package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.graphics.AnimationFlag
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.module.modules.render.GuiAnimation
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiOptionSlider
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.settings.GameSettings
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiOptionSlider::class)
class MixinGuiOptionSlider(buttonId: Int, x: Int, y: Int, buttonText: String) : GuiButton(buttonId, x, y, buttonText) {
    private val animation = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    @Shadow
    private var field_146134_p = 0f

    @Inject(method = ["<init>(IIILnet/minecraft/client/settings/GameSettings$Options;FF)V"], at = [At("RETURN")])
    fun `init$INJECT$RETURN`(buttonId: Int, x: Int, y: Int, optionIn: GameSettings.Options, minValueIn: Float, maxValue: Float, ci: CallbackInfo) {
        this.animation.forceUpdate(0.0f, 0.0f)
    }

    @Inject(method = ["mouseDragged"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiOptionSlider;drawTexturedModalRect(IIIIII)V", ordinal = 0)], cancellable = true)
    fun `mouseDragged$INJECT$HEAD`(mc: Minecraft, mouseX: Int, mouseY: Int, ci: CallbackInfo) {
        if (GuiAnimation.isEnabled) {
            ci.cancel()
            val renderSliderValue = this.animation.getAndUpdate(this.field_146134_p) * (this.width - 8).toFloat()
            val tessellator = Tessellator.getInstance()
            val bufferbuilder = tessellator.buffer
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX)
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue).toDouble(), this.yPosition.toDouble() + 20.0, this.zLevel.toDouble()).tex(0.0, 0.3359375).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue).toDouble() + 4.0, this.yPosition.toDouble() + 20.0, this.zLevel.toDouble()).tex(0.015625, 0.3359375).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue).toDouble() + 4.0, this.yPosition.toDouble() + 0.0, this.zLevel.toDouble()).tex(0.015625, 0.2578125).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue).toDouble(), this.yPosition.toDouble() + 0.0, this.zLevel.toDouble()).tex(0.0, 0.2578125).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue + 4.0f).toDouble(), this.yPosition.toDouble() + 20.0, this.zLevel.toDouble()).tex(0.765625, 0.3359375).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue + 8.0f).toDouble(), this.yPosition.toDouble() + 20.0, this.zLevel.toDouble()).tex(0.78125, 0.3359375).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue + 8.0f).toDouble(), this.yPosition.toDouble() + 0.0, this.zLevel.toDouble()).tex(0.78125, 0.2578125).endVertex()
            bufferbuilder.pos((this.xPosition.toFloat() + renderSliderValue + 4.0f).toDouble(), this.yPosition.toDouble() + 0.0, this.zLevel.toDouble()).tex(0.765625, 0.2578125).endVertex()
            tessellator.draw()
        }
    }
}
