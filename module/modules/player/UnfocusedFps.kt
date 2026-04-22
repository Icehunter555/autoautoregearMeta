package dev.wizard.meta.module.modules.player

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import org.lwjgl.opengl.Display
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object UnfocusedFps : Module(
    "UnfocusedFps",
    category = Category.PLAYER,
    description = "Reduces FPS when the game is running in the background"
) {
    private val fpsLimit by setting(this, IntegerSetting(settingName("FPS Limit"), 30, 1..120, 1))

    @JvmStatic
    fun handleGetLimitFramerate(cir: CallbackInfoReturnable<Int>) {
        if (INSTANCE.isDisabled) return
        if (Display.isActive()) return
        cir.returnValue = INSTANCE.fpsLimit
    }
}
