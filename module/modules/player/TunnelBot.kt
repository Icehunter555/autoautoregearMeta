package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.baritone.BaritoneCommandEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.movement.AutoWalk
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.RotationUtils
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.util.EnumFacing

object TunnelBot : Module(
    "TunnelBot",
    category = Category.PLAYER,
    description = "tunnels for you"
) {
    private val backFill by setting(this, BooleanSetting(settingName("BackFill"), false))
    private val height by setting(this, IntegerSetting(settingName("Height"), 2, 1..10, 1))
    private val width by setting(this, IntegerSetting(settingName("Width"), 1, 1..10, 1))

    private var lastDirection = EnumFacing.NORTH

    override fun isActive(): Boolean {
        if (!isEnabled) return false
        if (BaritoneUtils.isPathing()) return true
        return BaritoneUtils.primary?.builderProcess?.isActive ?: false
    }

    override fun getHudInfo(): String = lastDirection.name2.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    init {
        onDisable {
            BaritoneUtils.cancelEverything()
        }

        safeListener<TickEvent.Pre> {
            if (!isActive()) {
                sendTunnel()
            }
        }

        listener<BaritoneCommandEvent> {
            if (it.command.contains("stop") || it.command.contains("cancel")) {
                disable()
            }
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }

        val updateListener = {
            if (mc.player != null && isEnabled) {
                sendTunnel()
            }
        }
        height.valueListeners.add { _, _ -> updateListener() }
        width.valueListeners.add { _, _ -> updateListener() }

        backFill.valueListeners.add { _, it ->
            BaritoneUtils.settings?.backfill?.value = it
        }
    }

    private fun sendTunnel() {
        mc.player?.let {
            if (AutoWalk.isEnabled) AutoWalk.disable()
            BaritoneUtils.cancelEverything()

            val normalizedYaw = RotationUtils.normalizeAngle(it.rotationYaw)
            it.rotationYaw = Math.round(normalizedYaw / 90.0f).toFloat() * 90.0f
            it.rotationPitch = 0.0f

            lastDirection = it.horizontalFacing
            MessageSendUtils.sendBaritoneMessage("tunnel $height $width 100")
        }
    }
}
