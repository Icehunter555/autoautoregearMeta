package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.events.render.CameraSetupEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object FreeLook : Module(
    "FreeLook",
    category = Category.RENDER,
    description = "Look Freely"
) {
    private val arrowKeyYawAdjust by setting(this, BooleanSetting(settingName("Arrow Key Yaw Adjust"), false))
    private val arrowKeyYawAdjustIncrement by setting(this, FloatSetting(settingName("Yaw Adjust Increment"), 1.0f, 0.001f..10.0f, 0.001f, { arrowKeyYawAdjust }))

    private var cameraYaw = 0.0f
    private var cameraPitch = 0.0f
    private var thirdPersonBefore = 0

    init {
        onEnable {
            val safe = SafeClientEvent.instance ?: return@onEnable
            thirdPersonBefore = mc.gameSettings.thirdPersonView
            mc.gameSettings.thirdPersonView = 1
            cameraYaw = safe.player.rotationYaw + 180.0f
            cameraPitch = safe.player.rotationPitch
        }

        onDisable {
            mc.gameSettings.thirdPersonView = thirdPersonBefore
        }

        safeListener<CameraSetupEvent> {
            if (mc.gameSettings.thirdPersonView <= 0) return@safeListener
            it.yaw = cameraYaw
            it.pitch = cameraPitch
        }

        safeListener<InputUpdateEvent> {
            if (!arrowKeyYawAdjust) return@safeListener

            if (it.movementInput.leftKeyDown) {
                updateYaw(this, -arrowKeyYawAdjustIncrement)
                it.movementInput.leftKeyDown = false
            }
            if (it.movementInput.rightKeyDown) {
                updateYaw(this, arrowKeyYawAdjustIncrement)
                it.movementInput.rightKeyDown = false
            }
            it.movementInput.moveStrafe = 0.0f
        }
    }

    private fun updateYaw(event: SafeClientEvent, dYaw: Float) {
        cameraYaw += dYaw
        event.player.rotationYaw += dYaw
    }

    @JvmStatic
    fun handleTurn(entity: Entity, yaw: Float, pitch: Float, ci: CallbackInfo): Boolean {
        if (INSTANCE.isDisabled() || mc.player == null) return false

        if (entity == mc.player) {
            cameraYaw += yaw * 0.15f
            cameraPitch -= pitch * 0.15f
            cameraPitch = MathHelper.clamp(cameraPitch, -180.0f, 180.0f)
            ci.cancel()
            return true
        }
        return false
    }
}
