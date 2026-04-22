package dev.wizard.meta.mixins.core

import org.lwjgl.opengl.Display
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Display::class, remap = false)
class MixinDisplay {
    companion object {
        @Inject(method = ["setTitle"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun `setTitle$Inject$HEAD`(newTitle: String, ci: CallbackInfo) {
            ci.cancel()
        }
    }
}
