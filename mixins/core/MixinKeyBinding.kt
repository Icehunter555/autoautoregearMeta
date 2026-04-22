package dev.wizard.meta.mixins.core

import dev.wizard.meta.module.modules.player.BetterEat
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.settings.KeyBinding
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(KeyBinding::class)
class MixinKeyBinding {
    @Shadow
    private var field_74513_e = false

    @Inject(method = ["isKeyDown"], at = [At("HEAD")], cancellable = true)
    fun isKeyDownHead(cir: CallbackInfoReturnable<Boolean>) {
        val player = Wrapper.getPlayer()
        if (player != null && BetterEat.shouldCancelStopUsingItem() && this === Wrapper.getMinecraft().field_71474_y.field_74313_G) {
            cir.returnValue = field_74513_e
        }
    }
}
