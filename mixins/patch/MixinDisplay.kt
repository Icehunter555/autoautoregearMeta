package dev.wizard.meta.mixins.patch

import org.lwjgl.opengl.Display
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite

@Mixin(value = [Display::class], remap = false)
class MixinDisplay {
    companion object {
        @Overwrite
        @JvmStatic
        fun setTitle(newTitle: String) {
        }
    }
}
