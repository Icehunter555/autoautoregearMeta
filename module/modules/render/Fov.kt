package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.Bind
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object Fov : Module(
    "Fov",
    category = Category.RENDER,
    description = "Configures FOV"
) {
    private val fovValue by setting(this, FloatSetting(settingName("Fov"), 120.0f, 1.0f..180.0f, 0.5f))
    private val dynamicFov by setting(this, BooleanSetting(settingName("Dynamic Fov"), false))
    private val zoomBind by setting(this, BindSetting(settingName("Zoom Bind"), Bind(), consumer = {
        if (isEnabled && switchZoom && it) {
            zooming = !zooming
            updateSmoothCamera()
        }
    }))
    private val switchZoom by setting(this, BooleanSetting(settingName("Switch Zoom"), false, { !zoomBind.isEmpty() }))
    private val zoomFov by setting(this, FloatSetting(settingName("Zoom Fov"), 40.0f, 1.0f..180.0f, 0.5f, { !zoomBind.isEmpty() }))
    private val sensitivityMultiplier by setting(this, FloatSetting(settingName("Sensitivity Multiplier"), 1.0f, 0.1f..2.0f, 0.1f, { !zoomBind.isEmpty() }))
    private val smoothCamera by setting(this, BooleanSetting(settingName("Smooth Camera"), false, { !zoomBind.isEmpty() }))

    private var zooming = false

    override fun getHudInfo(): String = "%.1f".format(fovValue)

    private fun updateSmoothCamera() {
        if (smoothCamera) {
            mc.gameSettings.smoothCamera = zooming
        }
    }

    @JvmStatic
    fun getFOVModifierDynamicFov(value: Float): Float {
        return if (INSTANCE.isEnabled && INSTANCE.dynamicFov) INSTANCE.getFov() else value
    }

    @JvmStatic
    fun getFOVModifierNoDynamicFov(cir: CallbackInfoReturnable<Float>) {
        if (INSTANCE.isEnabled && !INSTANCE.dynamicFov) {
            cir.returnValue = INSTANCE.getFov()
        }
    }

    @JvmStatic
    fun getMouseSensitivity(value: Float): Float {
        return if (INSTANCE.isEnabled && zooming) {
            mc.gameSettings.mouseSensitivity * 0.6f * INSTANCE.sensitivityMultiplier + 0.2f
        } else value
    }

    private fun getFov(): Float = if (!zooming) fovValue else zoomFov

    init {
        onDisable {
            zooming = false
            mc.gameSettings.smoothCamera = false
        }

        listener<TickEvent.Pre> {
            if (zooming && smoothCamera) {
                mc.gameSettings.smoothCamera = true
            }
            if (!switchZoom) {
                zooming = zoomBind.isDown()
                updateSmoothCamera()
            }
        }
    }
}
