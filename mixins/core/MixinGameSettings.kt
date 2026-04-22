package dev.wizard.meta.mixins.core

import net.minecraft.client.settings.GameSettings
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GameSettings::class)
class MixinGameSettings {
    @Inject(method = ["setOptionValue"], at = [At("HEAD")], cancellable = true)
    fun `setOptionValue$Inject$HEAD`(settingsOption: GameSettings.Options, value: Int, ci: CallbackInfo) {
        if (settingsOption == GameSettings.Options.NARRATOR) {
            ci.cancel()
        }
    }
}
