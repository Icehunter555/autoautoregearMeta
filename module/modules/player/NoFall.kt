package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.movement.ElytraFly
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.Bind
import dev.wizard.meta.util.accessor.setOnGround
import dev.wizard.meta.util.world.getGroundLevel
import net.minecraft.network.play.client.CPacketPlayer

object NoFall : Module(
    "NoFall",
    category = Category.PLAYER,
    description = "Prevents fall damage"
) {
    private val distance by setting(this, IntegerSetting(settingName("Distance"), 3, 1..10, 1))
    var mode by setting(this, EnumSetting(settingName("Mode"), Mode.FALL))
    private val catchBind by setting(this, BindSetting(settingName("Anti-Fall bind"), Bind()))
    private val voidOnly by setting(this, BooleanSetting(settingName("Void Only"), false, { mode == Mode.CATCH }))

    private var hasSent = false

    init {
        onToggle {
            hasSent = false
        }

        safeListener<InputEvent.Keyboard> {
            if (!it.state) return@safeListener
            if (!hasSent && catchBind.isDown()) {
                player.fallDistance = 0.0f
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + 10.0, player.posZ, false))
                hasSent = true
            } else if (hasSent && !catchBind.isDown()) {
                hasSent = false
            }
        }

        safeListener<PacketEvent.Send> {
            if (it.packet is CPacketPlayer && noFallCheck(this)) {
                (it.packet as CPacketPlayer).setOnGround(true)
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (mode == Mode.CATCH && noFallCheck(this) && fallDistCheck(this)) {
                player.fallDistance = 0.0f
                connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + 10.0, player.posZ, false))
            }
        }
    }

    private fun noFallCheck(event: SafeClientEvent): Boolean {
        return !event.player.isCreative && !event.player.isSpectator && !event.player.isElytraFlying && !ElytraFly.isActive()
    }

    private fun fallDistCheck(event: SafeClientEvent): Boolean {
        return (!voidOnly && event.player.fallDistance >= distance) || (event.player.posY < 1.0 && event.world.getGroundLevel(event.player) == Double.MIN_VALUE)
    }

    enum class Mode { FALL, CATCH }
}
