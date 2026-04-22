package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.client.MainMenu
import dev.wizard.meta.util.KeyboardUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.*
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiMainMenu::class)
abstract class MixinGuiMainMenu : GuiScreen() {
    @Shadow
    private lateinit var field_73975_c: String

    @Inject(method = ["drawScreen"], at = [At("TAIL")])
    private fun addCustomText(mouseX: Int, mouseY: Int, partialTicks2: Float, ci: CallbackInfo) {
        if (MainMenu.addText) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("Meta Client", 2.0f, 5.0f, 0xFFFFFF)
        }
    }

    @Inject(method = ["keyTyped"], at = [At("HEAD")], cancellable = true)
    protected fun keyTyped_Inject(typedChar: Char, keyCode: Int, ci: CallbackInfo) {
        if (MainMenu.mainMenuKeybinds) {
            if (keyCode == KeyboardUtils.getKeyJava("m")) {
                Minecraft.getMinecraft().displayGuiScreen(GuiMultiplayer(this))
            }
            if (keyCode == KeyboardUtils.getKeyJava("s")) {
                Minecraft.getMinecraft().displayGuiScreen(GuiWorldSelection(this))
            }
            if (keyCode == KeyboardUtils.getKeyJava("o")) {
                Minecraft.getMinecraft().displayGuiScreen(GuiOptions(this, Minecraft.getMinecraft().gameSettings))
            }
            if (keyCode == KeyboardUtils.getKeyJava("q") || keyCode == KeyboardUtils.getKeyJava("e")) {
                Minecraft.getMinecraft().shutdown()
            }
        }
    }

    @Inject(method = ["initGui"], at = [At("RETURN")])
    private fun onInitGui(ci: CallbackInfo) {
        if (MainMenu.noRealmsButton) {
            this.buttonList.removeIf { button -> button.id == 14 }
            for (button2 in this.buttonList) {
                if (button2.id != 6) continue
                button2.width = 200
                button2.xPosition = this.width / 2 - 100
                break
            }
        }
        if (MainMenu.customSplash) {
            this.field_73975_c = MainMenu.sendCustomSplash()
        }
    }
}
