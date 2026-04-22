package dev.wizard.meta.mixins.core.gui

import dev.fastmc.common.MathUtil
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.module.modules.misc.ChatTweaks
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiNewChat
import net.minecraft.client.renderer.GlStateManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiNewChat::class)
abstract class MixinGuiNewChatScreen : Gui() {
    @Shadow
    private var field_146251_k = false

    @Unique
    private var `meta$animProgress` = 0f

    @Unique
    private var `meta$prevTime` = System.currentTimeMillis()

    @Unique
    private var `meta$animPercent` = 0f

    @Unique
    private fun `meta$updatePercentage`(diff: Long) {
        if (this.`meta$animProgress` < 1.0f) {
            this.`meta$animProgress` += 0.004f * diff.toFloat()
        }
        this.`meta$animProgress` = MathUtil.clamp(this.`meta$animProgress`, 0.0f, 1.0f)
    }

    @Inject(method = ["drawChat"], at = [At("HEAD")])
    private fun addAnimation(updateCounter: Int, ci: CallbackInfo) {
        val current = System.currentTimeMillis()
        val diff = current - this.`meta$prevTime`
        this.`meta$prevTime` = current
        this.`meta$updatePercentage`(diff)
        var t = this.`meta$animProgress`
        this.`meta$animPercent` = MathUtil.clamp(1.0f - (t - 1.0f).let { it * it * it * it }, 0.0f, 1.0f)
    }

    @Inject(method = ["drawChat"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", ordinal = 0, shift = At.Shift.AFTER)])
    private fun translate(ci: CallbackInfo) {
        var y = 1.0f
        if (!this.field_146251_k && ChatTweaks.isEnabled && ChatTweaks.chatAnimation) {
            y += (9.0f - 9.0f * this.`meta$animPercent`) * Wrapper.getMinecraft().gameSettings.chatScale
        }
        GlStateManager.translate(0.0f, y, 0.0f)
    }

    @Inject(method = ["printChatMessageWithOptionalDeletion"], at = [At("HEAD")])
    private fun resetPercentage(ci: CallbackInfo) {
        this.`meta$animProgress` = 0.0f
    }

    @Redirect(method = ["drawChat"], at = At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private fun drawStringWithShadow(instance: FontRenderer, text: String, x: Float, y: Float, color: Int): Int {
        if (text.contains("§(§)")) {
            Wrapper.getMinecraft().fontRendererObj.drawStringWithShadow(text, x, y, Settings.chatColor.rgb)
        } else {
            Wrapper.getMinecraft().fontRendererObj.drawStringWithShadow(text, x, y, color)
        }
        return 0
    }
}
