package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.MovementUtils
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.BlockPos

object FastSwim : Module(
    name = "Fast Swim",
    category = Category.MOVEMENT,
    description = "Swim faster",
    priority = 1010
) {
    private val water0 by setting("Water", true)
    private val waterHBoost by setting("Water H Boost", 6.0f, 1.0f..8.0f, 0.1f, LambdaUtilsKt.atTrue(water0))
    private val waterHSpeed by setting("Water H Speed", 5.75f, 0.01f..8.0f, 0.01f, LambdaUtilsKt.atTrue(water0))
    private val waterVBoost by setting("Water V Boost", 2.9f, 0.1f..8.0f, 0.1f, LambdaUtilsKt.atTrue(water0))
    private val waterUpSpeed by setting("Water Up Speed", 2.69f, 0.01f..8.0f, 0.01f, LambdaUtilsKt.atTrue(water0))
    private val waterDownSpeed by setting("Water Down Speed", 0.8f, 0.01f..2.0f, 0.01f, LambdaUtilsKt.atTrue(water0))
    
    private val lava0 by setting("Lava", true)
    private val lavaHBoost by setting("Lava H Boost", 4.0f, 1.0f..8.0f, 0.1f)
    private val lavaHSpeed by setting("Lava H Speed", 3.8f, 0.01f..8.0f, 0.01f)
    private val lavaVBoost by setting("Lava V Boost", 2.0f, 0.1f..8.0f, 0.1f)
    private val lavaUpSpeed by setting("Lava Up Speed", 2.69f, 0.01f..8.0f, 0.01f)
    private val lavaDownSpeed by setting("Lava Down Speed", 4.22f, 0.01f..8.0f, 0.01f)
    
    private val jitter by setting("Jitter", 8, 1..20, 1)
    private val timerBoost by setting("Timer Boost", 1.09f, 1.0f..1.5f, 0.01f)

    private var moveSpeed = 0.0
    private var motionY = 0.0

    override fun getHudInfo(): String {
        return "$lavaHSpeed, $lavaVBoost"
    }

    private fun SafeClientEvent.runFastSwim(): Boolean {
        if (player.capabilities.isFlying || ElytraFly.isActive()) return false

        if (isInLiquid(Material.LAVA)) {
            if (!lava0) return false
            lavaSwim()
        } else if (isInLiquid(Material.WATER)) {
            if (!water0) return false
            waterSwim()
        } else {
            return false
        }
        return true
    }

    private fun SafeClientEvent.isInLiquid(material: Material): Boolean {
        val box = player.entityBoundingBox
        val pos = BlockPos.PooledMutableBlockPos.retain()
        for (x in MathUtilKt.floorToInt(box.minX + 0.1)..MathUtilKt.floorToInt(box.maxX - 0.1)) {
            for (y in MathUtilKt.floorToInt(box.minY + 0.5)..MathUtilKt.floorToInt(box.maxY - 0.25)) {
                for (z in MathUtilKt.floorToInt(box.minZ + 0.1)..MathUtilKt.floorToInt(box.maxZ - 0.1)) {
                    val blockState = world.getBlockState(pos.setPos(x, y, z))
                    if (blockState.block is BlockLiquid && blockState.material == material) {
                        if (BlockLiquid.getLiquidHeightPercent(blockState.block.metaFromState(blockState)) - player.posY < 0.2) {
                            pos.release()
                            return true
                        }
                    }
                }
            }
        }
        pos.release()
        return false
    }

    private fun SafeClientEvent.lavaSwim() {
        ySwim(lavaVBoost, lavaUpSpeed, lavaDownSpeed)
        if (MovementUtils.isInputting()) {
            TimerManager.modifyTimer(this@FastSwim, 50.0f / timerBoost)
        }
        if (MovementUtils.isInputting()) {
            val yaw = MovementUtils.calcMoveYaw(player)
            moveSpeed = Math.min(Math.max(moveSpeed * lavaHBoost, 0.05), lavaHSpeed.toDouble() / 20.0)
            player.motionX = -Math.sin(yaw) * moveSpeed
            player.motionZ = Math.cos(yaw) * moveSpeed
        } else {
            stopMotion()
        }
    }

    private fun SafeClientEvent.waterSwim() {
        ySwim(waterVBoost, waterUpSpeed, waterDownSpeed * 20.0f)
        if (MovementUtils.isInputting()) {
            TimerManager.modifyTimer(this@FastSwim, 50.0f / timerBoost)
        }
        if (MovementUtils.isInputting()) {
            val yaw = MovementUtils.calcMoveYaw(player)
            val multiplier = MovementUtils.getSpeedEffectMultiplier(player as EntityLivingBase)
            moveSpeed = Math.min(Math.max(moveSpeed * waterHBoost, 0.075), waterHSpeed.toDouble() / 20.0)
            if (player.movementInput.sneak && !player.movementInput.jump) {
                val downMotion = player.motionY * 0.25
                moveSpeed = Math.min(moveSpeed, Math.max(moveSpeed + downMotion, 0.0))
            }
            player.motionX = -Math.sin(yaw) * moveSpeed * multiplier
            player.motionZ = Math.cos(yaw) * moveSpeed * multiplier
        } else {
            stopMotion()
        }
    }

    private fun SafeClientEvent.ySwim(vBoost: Float, upSpeed: Float, downSpeed: Float) {
        val jump = player.movementInput.jump
        val sneak = player.movementInput.sneak
        motionY = if (jump xor sneak) {
            if (jump) Math.min(motionY + vBoost.toDouble() / 20.0, upSpeed.toDouble() / 20.0)
            else Math.max(motionY - vBoost.toDouble() / 20.0, (-downSpeed).toDouble() / 20.0)
        } else {
            val y = Math.pow(0.1, jitter.toDouble())
            if (MathUtilKt.isEven(player.ticksExisted)) -y else y
        }
        player.motionY = motionY
    }

    private fun stopMotion() {
        mc.player?.let {
            it.motionX = 0.0
            it.motionZ = 0.0
        }
        moveSpeed = 0.0
    }

    private fun reset() {
        moveSpeed = 0.0
        motionY = 0.0
    }

    init {
        onDisable {
            reset()
        }

        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) {
                reset()
            }
        }

        listener<PlayerMoveEvent.Pre>(-1000) {
            if (!runFastSwim()) {
                reset()
            }
        }
    }
}
