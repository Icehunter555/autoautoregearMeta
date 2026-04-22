package dev.wizard.meta.module.modules.combat

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.PlayerAttackEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.translation.TranslateType
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.isInWeb
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumHand
import net.minecraft.world.GameType

object Criticals : Module(
    "Criticals",
    category = Category.COMBAT,
    description = "Always do critical attacks"
) {
    private val mode0 = setting(this, EnumSetting(settingName("Mode"), Mode.PACKET))
    private val onlyAura by setting(this, BooleanSetting(settingName("Only Aura"), true))
    private val jumpMotion by setting(this, DoubleSetting(settingName("Jump Motion"), 0.25, 0.1..0.5, 0.01, { mode0.value == Mode.MINI_JUMP }))
    private val attackFallDistance by setting(this, DoubleSetting(settingName("Attack Fall Distance"), 0.1, 0.05..1.0, 0.05, { mode0.value != Mode.PACKET }))

    private var delayTick = -1
    private var target: Entity? = null
    private var attacking = false

    init {
        onDisable { reset() }

        listener<PacketEvent.Send> {
            if (it.packet is CPacketAnimation && mode0.value != Mode.PACKET && delayTick > -1) {
                it.cancel()
            }
        }

        safeListener<PlayerAttackEvent> {
            if (it.cancelled || attacking || it.entity !is EntityLivingBase || !canDoCriticals(true) || (onlyAura && !KillAura.isActive())) {
                return@safeListener
            }

            val cooldownReady = player.onGround && player.getCooledAttackStrength(0.5f) > 0.9f
            if (!cooldownReady) return@safeListener

            when (mode0.value) {
                Mode.PACKET -> {
                    connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY + 0.1, player.posZ, false))
                    connection.sendPacket(CPacketPlayer.Position(player.posX, player.posY, player.posZ, false))
                }
                Mode.JUMP -> jumpAndCancel(this, it, null)
                Mode.MINI_JUMP -> jumpAndCancel(this, it, jumpMotion)
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (mode0.value == Mode.PACKET || delayTick <= -1) return@safeParallelListener

            delayTick--
            if (target != null && player.fallDistance.toDouble() >= attackFallDistance && canDoCriticals(!player.onGround)) {
                val t = target
                reset()
                if (t != null) {
                    attacking = true
                    connection.sendPacket(CPacketUseEntity(t))
                    connection.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
                    attacking = false
                }
            }
        }
    }

    override fun isActive(): Boolean = isEnabled && !delaying()

    override fun getHudInfo(): String = mode0.value.displayString

    private fun reset() {
        delayTick = -1
        target = null
    }

    private fun jumpAndCancel(event: SafeClientEvent, attackEvent: PlayerAttackEvent, motion: Double?) {
        if (!delaying()) {
            event.player.jump()
            motion?.let { event.player.motionY = it }
            target = attackEvent.entity
            if (event.mc.playerController.currentGameType != GameType.SPECTATOR) {
                event.player.attackTargetEntityWithCurrentItem(attackEvent.entity)
                event.player.resetCooldown()
            }
            delayTick = 20
        }
        attackEvent.cancel()
    }

    private fun delaying(): Boolean = mode0.value != Mode.PACKET && delayTick > -1 && target != null

    private fun SafeClientEvent.canDoCriticals(onGround: Boolean): Boolean {
        return onGround && !player.isInWeb && !player.isOnLadder && !player.isRiding && !player.isPotionActive(MobEffects.BLINDNESS) && !EntityUtils.isInOrAboveLiquid(player)
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        PACKET(TranslateType.COMMON.commonKey("Packet")),
        JUMP(TranslateType.COMMON.commonKey("Jump")),
        MINI_JUMP(TranslateType.COMMON.commonKey("Mini Jump"))
    }
}
