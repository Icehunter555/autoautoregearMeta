package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.misc.ForceDisconnect
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiIngameMenu::class)
class MixinGuiIngameMenu : GuiScreen() {
    @Inject(method = ["actionPerformed"], at = [At("HEAD")], cancellable = true)
    fun actionPerformed(button: GuiButton, ci: CallbackInfo) {
        when (button.id) {
            1 -> {
                if (ForceDisconnect.isEnabled) {
                    ForceDisconnect.handleButtonPress(ci)
                }
            }
            -2147483648 -> {
                Wrapper.getMinecraft().displayGuiScreen(GuiMultiplayer(this))
            }
        }
    }
}
