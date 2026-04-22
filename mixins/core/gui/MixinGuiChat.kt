package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.gui.mc.TrollGuiChat
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiChat::class)
abstract class MixinGuiChat : GuiScreen() {
    @Shadow
    protected lateinit var field_146415_a: GuiTextField

    @Shadow
    private lateinit var field_146410_g: String

    @Shadow
    private var field_146416_h = 0

    @Inject(method = ["keyTyped(CI)V"], at = [At("RETURN")])
    fun returnKeyTyped(typedChar: Char, keyCode: Int, info: CallbackInfo) {
        val currentScreen = Wrapper.getMinecraft().field_71462_r
        if (currentScreen is GuiChat && currentScreen !is TrollGuiChat && this.field_146415_a.text.startsWith(CommandManager.prefix)) {
            Wrapper.getMinecraft().displayGuiScreen(TrollGuiChat(this.field_146415_a.text, this.field_146410_g, this.field_146416_h))
        }
    }
}
