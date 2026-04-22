package dev.wizard.meta.mixins.core.entity

import dev.wizard.meta.module.modules.movement.EntitySpeed
import net.minecraft.entity.passive.EntityPig
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(EntityPig::class)
class MixinEntityPig {
    @Inject(method = ["canBeSteered"], at = [At("HEAD")], cancellable = true)
    fun canBeSteered(returnable: CallbackInfoReturnable<Boolean>) {
        if (EntitySpeed.isEnabled) {
            returnable.returnValue = true
            returnable.cancel()
        }
    }
}
