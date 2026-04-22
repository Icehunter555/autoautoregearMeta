package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerPushOutOfBlockEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.accessor.NetworkKt
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.network.play.server.SPacketEntityStatus
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object Velocity : Module(
    name = "Velocity",
    alias = arrayOf("AntiKnockBack", "NoPush"),
    category = Category.MOVEMENT,
    description = "Modify player velocity",
    priority = 1008
) {
    private val horizontal by setting("Horizontal", 0.0f, -5.0f..5.0f, 0.05f)
    private val vertical by setting("Vertical", 0.0f, -5.0f..5.0f, 0.05f)
    private val noPush0 by setting("No Push", true)
    private val entity by setting("Entity", true, LambdaUtilsKt.atTrue(noPush0))
    private val liquid by setting("Liquid", true, LambdaUtilsKt.atTrue(noPush0))
    private val block by setting("Block", true, LambdaUtilsKt.atTrue(noPush0))
    private val pushable by setting("Pushable", true, LambdaUtilsKt.atTrue(noPush0))
    private val fishingRod by setting("Fishing Rod", true, LambdaUtilsKt.atTrue(noPush0))
    private val shulkerBox by setting("Shulker Box", false, LambdaUtilsKt.atTrue(noPush0))
    private val slimeBounce by setting("Slime Bounce", false)
    val antiPiston by setting("Anti Piston", false)

    override fun getHudInfo(): String {
        return "$horizontal/$vertical"
    }

    private fun isZero(): Boolean {
        return horizontal == 0.0f && vertical == 0.0f
    }

    @JvmStatic
    fun handleApplyEntityCollision(entity1: Entity, entity2: Entity, ci: CallbackInfo) {
        if (isDisabled || !noPush0 || !entity) return
        if (entity1.isRidingSameEntity(entity2) || entity1.noClip || entity2.noClip) return
        val player = mc.player ?: return
        if (entity1 != player && entity2 != player) return

        var x = entity2.posX - entity1.posX
        var z = entity2.posZ - entity1.posZ
        var dist = Math.max(Math.abs(x), Math.abs(z))
        if (dist < 0.01) return

        dist = Math.sqrt(dist)
        x /= dist
        z /= dist
        val multiplier = Math.min(1.0 / dist, 1.0)
        val collisionReduction = 1.0f - entity1.entityCollisionReduction
        addCollisionVelocity(entity1, player, -(x * multiplier * 0.05 * collisionReduction.toDouble()), -(z * multiplier * 0.05 * collisionReduction.toDouble()))
        addCollisionVelocity(entity2, player, x * multiplier * 0.05 * collisionReduction.toDouble(), z * multiplier * 0.05 * collisionReduction.toDouble())
        ci.cancel()
    }

    private fun addCollisionVelocity(target: Entity, player: EntityPlayerSP, x: Double, z: Double) {
        if (target != player && !target.isRiding) {
            target.motionX += x
            target.motionZ += z
            target.isAirBorne = true
        }
    }

    @JvmStatic
    fun shouldCancelLiquidVelocity(): Boolean {
        return isEnabled && noPush0 && liquid
    }

    @JvmStatic
    fun shouldCancelMove(): Boolean {
        return isEnabled && pushable
    }

    @JvmStatic
    fun shouldCancelShulkerPush(): Boolean {
        return isEnabled && noPush0 && shulkerBox
    }

    @JvmStatic
    fun shouldCancelSlime(): Boolean {
        return isEnabled && slimeBounce
    }

    @JvmStatic
    fun shouldCancelPiston(): Boolean {
        return isEnabled && antiPiston
    }

    init {
        listener<PacketEvent.Receive>(-1000) { event ->
            val packet = event.packet
            if (packet is SPacketEntityVelocity) {
                if (packet.entityID != player.entityId) return@listener
                if (isZero()) {
                    event.cancel()
                } else {
                    NetworkKt.setPacketMotionX(packet, (NetworkKt.getPacketMotionX(packet).toFloat() * horizontal).toInt())
                    NetworkKt.setPacketMotionY(packet, (NetworkKt.getPacketMotionY(packet).toFloat() * vertical).toInt())
                    NetworkKt.setPacketMotionZ(packet, (NetworkKt.getPacketMotionZ(packet).toFloat() * horizontal).toInt())
                }
            } else if (packet is SPacketExplosion) {
                if (isZero()) {
                    event.cancel()
                } else {
                    NetworkKt.setPacketMotionX(packet, NetworkKt.getPacketMotionX(packet) * horizontal.toDouble())
                    NetworkKt.setPacketMotionY(packet, NetworkKt.getPacketMotionY(packet) * vertical.toDouble())
                    NetworkKt.setPacketMotionZ(packet, NetworkKt.getPacketMotionZ(packet) * horizontal.toDouble())
                }
            } else if (packet is SPacketEntityStatus && packet.opCode.toInt() == 31 && fishingRod) {
                event.cancel()
                mc.addScheduledTask {
                    val connection = mc.connection ?: return@addScheduledTask
                    val entity = packet.getEntity(world)
                    if (entity is EntityFishHook) {
                        if (entity.caughtEntity == null || entity.caughtEntity != player) {
                            packet.processPacket(connection)
                        }
                    } else {
                        packet.processPacket(connection)
                    }
                }
            }
        }

        listener<PlayerPushOutOfBlockEvent> {
            if (block) {
                it.cancel()
            }
        }
    }
}
