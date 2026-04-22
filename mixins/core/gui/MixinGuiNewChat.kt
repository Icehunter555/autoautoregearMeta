package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.misc.ChatTweaks
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiNewChat
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.Redirect
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiNewChat::class)
abstract class MixinGuiNewChat {
    @Shadow
    @Final
    private lateinit var field_146252_h: MutableList<ChatLine>

    @Shadow
    @Final
    private lateinit var field_146253_i: MutableList<ChatLine>

    @Inject(method = ["setChatLine"], at = [At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0, remap = false)], cancellable = true)
    fun setChatLineInvokeSize(chatComponent: ITextComponent, chatLineId: Int, updateCounter: Int, displayOnly: Boolean, ci: CallbackInfo) {
        ChatTweaks.handleSetChatLine(this.field_146253_i, this.field_146252_h, chatComponent, chatLineId, updateCounter, displayOnly, ci)
    }

    @Inject(method = ["drawChat"], at = [At("HEAD")])
    private fun preDrawChat(updateCounter: Int, ci: CallbackInfo) {
    }

    @Redirect(method = ["drawChat"], at = At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"))
    private fun drawRectBackgroundClean(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        if (ChatTweaks.isEnabled) {
            if (!ChatTweaks.cleanChat) {
                if (ChatTweaks.doChatColor) {
                    Gui.drawRect(left, top, right, bottom, ChatTweaks.theChatColor)
                } else {
                    Gui.drawRect(left, top, right, bottom, color)
                }
            }
        } else {
            Gui.drawRect(left, top, right, bottom, color)
        }
    }
}
