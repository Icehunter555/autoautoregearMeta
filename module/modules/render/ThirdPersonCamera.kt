package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.accessor.unpressKey

object ThirdPersonCamera : Module(
    "NoCameraClip",
    category = Category.RENDER,
    description = "Modify 3rd person camera behavior"
) {
    val cameraClip by setting(this, BooleanSetting(settingName("Camera Clip"), true))
    private val whileHolding by setting(this, BooleanSetting(settingName("While Holding"), false))
    private val perspectiveMode by setting(this, EnumSetting(settingName("PerspectiveMode Mode"), PerspectiveMode.BACK, { whileHolding }))
    val distance by setting(this, FloatSetting(settingName("Camera Distance"), 4.0f, 1.0f..10.0f, 0.1f, description = "Camera distance to player"))

    init {
        listener<InputEvent.Keyboard> {
            if (whileHolding && Freecam.isDisabled && it.key == mc.gameSettings.keyBindTogglePerspective.keyCode) {
                mc.gameSettings.thirdPersonView = if (it.state) perspectiveMode.state else 0
                mc.gameSettings.keyBindTogglePerspective.unpressKey()
            }
        }
    }

    private enum class PerspectiveMode(val state: Int) { BACK(1), FRONT(2) }
}
