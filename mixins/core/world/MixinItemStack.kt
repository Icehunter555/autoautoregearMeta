package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.render.Glint
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(value = [ItemStack::class], priority = -500)
class MixinItemStack {
    @Inject(method = ["hasEffect"], at = [At("HEAD")], cancellable = true)
    private fun forceEnchantGlint(cir: CallbackInfoReturnable<Boolean>) {
        val stack = this as ItemStack
        if (stack.isEmpty) {
            return
        }
        val item = stack.item
        if (Glint.isEnabled) {
            Glint.handleItemGlint(item, cir)
        }
    }
}
