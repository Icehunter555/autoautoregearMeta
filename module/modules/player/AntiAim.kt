package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.manager.managers.PlayerPacketManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.vector.Vec2f

object AntiAim : Module(
    "AntiAim",
    category = Category.PLAYER,
    description = "Advanced view angle manipulation"
) {
    private val headOnly by setting(this, BooleanSetting(settingName("Head Only"), false))
    private val yawMode by setting(this, EnumSetting(settingName("Yaw Mode"), AimMode.NONE))
    private val pitchMode by setting(this, EnumSetting(settingName("Pitch Mode"), AimMode.NONE))
    private val speed by setting(this, IntegerSetting(settingName("Speed"), 1, 1..45, 1))
    private val yawDelta by setting(this, FloatSetting(settingName("Yaw Delta"), 60.0f, -360.0f..360.0f, 10.0f))
    private val pitchDelta by setting(this, FloatSetting(settingName("Pitch Delta"), 10.0f, -90.0f..90.0f, 10.0f))
    private val allowInteract by setting(this, BooleanSetting(settingName("Allow Interact"), true))

    private var rotationPitch = 0.0f
    private var rotationYaw = 0.0f
    private var pitchSinusStep = 0.0f
    private var yawSinusStep = 0.0f

    override fun getHudInfo(): String = "${yawMode.displayName}, ${pitchMode.displayName}"

    init {
        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (allowInteract && (mc.gameSettings.keyBindUseItem.isKeyDown || mc.gameSettings.keyBindAttack.isKeyDown)) return@safeListener

            val finalYaw = if (yawMode != AimMode.NONE) rotationYaw else player.rotationYaw
            val finalPitch = if (pitchMode != AimMode.NONE) rotationPitch else player.rotationPitch

            val builder = PlayerPacketManager.Packet.Builder(getModulePriority())
            builder.rotate(Vec2f(finalYaw, finalPitch))
            val packet = builder.build()
            if (packet != null) {
                PlayerPacketManager.sendPlayerPacket(packet)
            }

            if (!headOnly && yawMode != AimMode.NONE) {
                player.rotationYawHead = finalYaw
                player.renderYawOffset = finalYaw
            }
        }

        safeListener<TickEvent.Post> {
            if (player.ticksExisted % speed != 0) return@safeListener

            when (pitchMode) {
                AimMode.NONE -> rotationPitch = player.rotationPitch
                AimMode.RANDOMANGLE -> rotationPitch = (Math.random() * 180 - 90).toFloat()
                AimMode.SPIN -> {
                    rotationPitch += pitchDelta
                    if (rotationPitch > 90.0f) rotationPitch = -90.0f
                    else if (rotationPitch < -90.0f) rotationPitch = 90.0f
                }
                AimMode.SINUS -> {
                    pitchSinusStep += speed / 10.0f
                    rotationPitch = (player.rotationPitch + pitchDelta * Math.sin(pitchSinusStep.toDouble())).toFloat().coerceIn(-90.0f, 90.0f)
                }
                AimMode.FIXED -> rotationPitch = pitchDelta.coerceIn(-90.0f, 90.0f)
                AimMode.STATIC -> rotationPitch = (player.rotationPitch + pitchDelta).coerceIn(-90.0f, 90.0f)
            }

            when (yawMode) {
                AimMode.NONE -> rotationYaw = player.rotationYaw
                AimMode.RANDOMANGLE -> rotationYaw = (Math.random() * 360).toFloat()
                AimMode.SPIN -> {
                    rotationYaw += yawDelta
                    if (rotationYaw > 360.0f) rotationYaw -= 360.0f
                    else if (rotationYaw < 0.0f) rotationYaw += 360.0f
                }
                AimMode.SINUS -> {
                    yawSinusStep += speed / 10.0f
                    rotationYaw = player.rotationYaw + (yawDelta * Math.sin(yawSinusStep.toDouble())).toFloat()
                }
                AimMode.FIXED -> rotationYaw = yawDelta
                AimMode.STATIC -> rotationYaw = (player.rotationYaw % 360.0f) + yawDelta
            }
        }
    }

    private enum class AimMode(override val displayName: CharSequence) : DisplayEnum {
        NONE("None"),
        RANDOMANGLE("Random Angle"),
        SPIN("Spin"),
        SINUS("Sinus"),
        FIXED("Fixed"),
        STATIC("Static")
    }
}
