package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.render.NoRender
import dev.wizard.meta.module.modules.render.Tooltips
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiScreen::class)
class MixinGuiScreen {
    @Unique
    private var hoveringShulker = false

    @Unique
    private var shulkerStack: ItemStack = ItemStack.EMPTY

    @Unique
    private var shulkerName: String = ""

    @Shadow
    protected lateinit var buttonList: MutableList<GuiButton>

    @Inject(method = ["renderToolTip"], at = [At("HEAD")], cancellable = true)
    fun renderToolTip(stack: ItemStack, x: Int, y: Int, info: CallbackInfo) {
        if (Tooltips.isShulkerEnabled) {
            if (stack.item is ItemShulkerBox) {
                val tagCompound = Tooltips.getShulkerData(stack)
                this.shulkerName = stack.displayName
                this.shulkerStack = stack
                this.hoveringShulker = true
                if (tagCompound != null) {
                    info.cancel()
                    Tooltips.renderShulkerAndItems(stack, x, y, tagCompound)
                }
            } else {
                this.hoveringShulker = false
            }
        }
    }

    @Inject(method = ["keyTyped"], at = [At("HEAD")], cancellable = true)
    fun isPeekBindPressed(typedChar: Char, keyCode: Int, ci: CallbackInfo) {
        if (Tooltips.isShulkerEnabled && keyCode == Tooltips.peekBind.key && this.hoveringShulker) {
            Tooltips.drawPeekGui(this.shulkerStack, this.shulkerName)
        }
    }

    @Inject(method = ["drawWorldBackground"], at = [At("HEAD")], cancellable = true)
    private fun cancelBackgroundRendering(tint: Int, ci: CallbackInfo) {
        if (Wrapper.getWorld() != null && NoRender.isEnabled && NoRender.noGuiBackground) {
            ci.cancel()
        }
    }
}
