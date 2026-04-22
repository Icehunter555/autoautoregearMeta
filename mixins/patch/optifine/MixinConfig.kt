package dev.wizard.meta.mixins.patch.optifine

import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Pseudo
@Mixin(targets = ["Config"], remap = false)
class MixinConfig {
    companion object {
        @Inject(method = ["isFastRender"], at = [At("HEAD")], cancellable = true, remap = false)
        @JvmStatic
        private fun isFastRender(isFastRender: CallbackInfoReturnable<Boolean>) {
            isFastRender.returnValue = false
        }
    }
}
