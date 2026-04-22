package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.player.CustomDeathReason
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Mutable
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiGameOver::class)
class MixinGuiGameOver {
    @Mutable
    @Final
    @Shadow
    private lateinit var field_184871_f: ITextComponent

    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun init(cause: ITextComponent, ci: CallbackInfo) {
        if (CustomDeathReason.isEnabled) {
            this.field_184871_f = CustomDeathReason.reason
        }
    }
}
