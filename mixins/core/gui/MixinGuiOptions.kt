package dev.wizard.meta.mixins.core.gui

import net.minecraft.client.gui.GuiOptions
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiOptions::class)
class MixinGuiOptions {
    @Shadow
    protected lateinit var field_146442_a: String

    @Inject(method = ["initGui"], at = [At("RETURN")])
    private fun setTitle(ci: CallbackInfo) {
        this.field_146442_a = "Settings"
    }
}
