package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.misc.AltProtect
import dev.wizard.meta.module.modules.render.Cosmetics
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(AbstractClientPlayer::class)
abstract class MixinAbstractClientPlayer {
    @Inject(method = ["getLocationCape"], at = [At("HEAD")], cancellable = true)
    fun getLocationCape(callbackInfoReturnable: CallbackInfoReturnable<ResourceLocation>) {
        val player = this as AbstractClientPlayer
        if (Cosmetics.isEnabled) {
            Cosmetics.handlePlayer(player, callbackInfoReturnable)
        }
    }

    @Inject(method = ["getLocationSkin()Lnet/minecraft/util/ResourceLocation;"], at = [At("HEAD")], cancellable = true)
    fun getLocationSkin(cir: CallbackInfoReturnable<ResourceLocation>) {
        val player = this as AbstractClientPlayer
        if (AltProtect.isEnabled && AltProtect.skinProtect) {
            AltProtect.handleSkin(player, cir)
        }
    }
}
