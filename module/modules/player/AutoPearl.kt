package dev.wizard.meta.module.modules.player

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.inventory.slot.firstItem
import dev.wizard.meta.util.inventory.slot.hotbarSlots
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.distanceTo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import java.util.HashSet

object AutoPearl : Module(
    "AutoPearl",
    category = Category.PLAYER,
    description = "Automatically throws ender pearls to chase players",
    modulePriority = 200
) {
    private val chasePearls by setting(this, BooleanSetting(settingName("Chase Pearls"), true))
    private val pearlsRange by setting(this, FloatSetting(settingName("Range"), 50.0f, 5.0f..100.0f, 5.0f, { chasePearls }))
    private val pearlOnEnemy by setting(this, BooleanSetting(settingName("Pearl On Enemy"), true))
    private val enemyPearlDelay by setting(this, IntegerSetting(settingName("Pearl On Enemy Delay"), 100, 1..10000, 100, { pearlOnEnemy }))
    private val enemyPearlMaxDistance by setting(this, FloatSetting(settingName("Max Enemy Distance"), 40.0f, 32.0f..100.0f, 5.0f, { pearlOnEnemy }))
    private val enemyPearlMinDistance by setting(this, FloatSetting(settingName("Min Enemy Distance"), 20.0f, 1.0f..31.0f, 5.0f, { pearlOnEnemy }))

    private val processedPearlIds = HashSet<Int>()
    private val delayTimer = TickTimer(TimeUnit.MILLISECONDS)
    private var originalYaw = 0.0f
    private var originalPitch = 0.0f

    override fun getHudInfo(): String = pearlsRange.toString()

    init {
        onDisable {
            processedPearlIds.clear()
        }

        safeListener<TickEvent.Post> {
            for (entity in world.playerEntities) {
                if (EntityUtils.isFriend(entity) || EntityUtils.isSelf(entity)) continue
                val dist = player.getDistance(entity)
                if (dist > enemyPearlMaxDistance || dist < enemyPearlMinDistance) continue
                if (CombatManager.target != entity) continue
                if (!delayTimer.tickAndReset(enemyPearlDelay.toLong())) continue

                val rotation = RotationUtils.getRotationToEntity(this, entity)
                throwPearl(this, rotation.x, rotation.y)
            }

            for (entity in world.loadedEntityList) {
                if (entity !is EntityEnderPearl || processedPearlIds.contains(entity.entityId)) continue

                var detected = false
                for (other in world.playerEntities) {
                    if (other == player || FriendManager.isFriend(other.name)) continue

                    val eyePos = other.getPositionEyes(1.0f)
                    val pearlPos = entity.positionVector
                    val toPearl = pearlPos.subtract(eyePos)
                    if (toPearl.lengthVector() > pearlsRange) continue

                    val lookVec = other.getLook(1.0f)
                    if (toPearl.normalize().dotProduct(lookVec) < 0.0) continue

                    detected = true
                    break
                }

                if (detected && chasePearls) {
                    computeRotationsFromVelocity(entity)?.let {
                        throwPearl(this, it[0], it[1])
                        processedPearlIds.add(entity.entityId)
                    }
                }
            }
        }
    }

    private fun throwPearl(event: SafeClientEvent, yaw: Float, pitch: Float) {
        originalYaw = event.player.rotationYaw
        originalPitch = event.player.rotationPitch

        val slot = event.player.hotbarSlots.firstItem(Items.ENDER_PEARL) ?: return

        HotbarSwitchManager.ghostSwitch(event, HotbarSwitchManager.Override.NONE, slot) {
            connection.sendPacket(CPacketPlayer.Rotation(yaw, pitch, event.player.onGround))
            mc.playerController.processRightClick(event.player, event.world, EnumHand.MAIN_HAND)
        }

        event.player.rotationYaw = originalYaw
        event.player.rotationPitch = originalPitch
    }

    private fun computeRotationsFromVelocity(pearl: EntityEnderPearl): FloatArray? {
        val vx = pearl.motionX
        val vy = pearl.motionY
        val vz = pearl.motionZ
        val horiz = Math.sqrt(vx * vx + vz * vz)
        if (horiz == 0.0) return null

        val yaw = (Math.toDegrees(Math.atan2(vz, vx)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(Math.atan2(vy, horiz))).toFloat()
        return floatArrayOf(yaw, pitch)
    }
}
