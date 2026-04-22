package dev.wizard.meta.mixins.patch.baritone

import baritone.api.Settings
import dev.wizard.meta.event.events.baritone.BaritoneSettingsInitEvent
import dev.wizard.meta.util.BaritoneUtils
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [Settings::class], remap = false)
class MixinBaritoneSettings {
    @Inject(method = ["<init>"], at = [At("RETURN")])
    private fun baritoneSettingsInit(ci: CallbackInfo) {
        BaritoneUtils.isInitialized = true
        BaritoneSettingsInitEvent.post()
    }
}
