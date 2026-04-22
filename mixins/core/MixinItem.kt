package dev.wizard.meta.mixins.core

import dev.wizard.meta.module.modules.player.HandSwing
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Item::class)
class MixinItem {
    @Inject(method = ["shouldCauseReequipAnimation"], at = [At("HEAD")], cancellable = true, remap = false)
    private fun shouldCauseReequipAnimation(oldStack: ItemStack, newStack: ItemStack, slotChanged: Boolean, cir: CallbackInfoReturnable<Boolean>) {
        if (HandSwing.isDisabled || !HandSwing.cancelEquipAnimation) {
            return
        }
        cir.returnValue = slotChanged && !oldStack.equals(newStack)
    }
}
