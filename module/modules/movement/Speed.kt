package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.MathUtilKt
import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.StepEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.events.player.PlayerTravelEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.exploit.Burrow
import dev.wizard.meta.module.modules.exploit.PacketFlyOld
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.accessor.isInWeb
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.math.vector.Vec2d
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import java.util.concurrent.CopyOnWriteArrayList

object Speed : Module(
    name = "Speed",
    category = Category.MOVEMENT,
    description = "Improves control in air",
    modulePriority = 100
) {
    private val page by setting("Page", Page.STRAFE)
    private val bbtt by setting("2B2T", false, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val timerBoost by setting("Timer Boost", 1.09f, 1.0f..1.5f, 0.01f, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val baseSpeed by setting("Base Speed", 0.2873, 0.0..0.5, 1.0E-4, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val maxStepSpeed by setting("Max Step Speed", 0.35, 0.0..0.5, 0.01, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val stepTimeout by setting("Step Timeout", 10, 0..100, 1, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val maxSpeed by setting("Max Speed", 1.0, 0.1..2.0, 0.01, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val airDecay by setting("Air Decay", 0.9937, 0.0..1.0, 1.0E-4, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val autoJump by setting("Auto Jump", true, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val jumpMotion by setting("Jump Motion", 0.4, 0.1..0.5, 0.01, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val jumpBoost by setting("Jump Boost", 2.0f, 1.0f..4.0f, 0.01f, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val jumpDecay by setting("Jump Decay", 0.66f, 0.0f..1.0f, 0.01f, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val maxJumpSpeed by setting("Max Jump Speed", 0.548, 0.1..2.0, 0.01, LambdaUtilsKt.atValue(page, Page.STRAFE))
    private val jumpDelay by setting("Jump Delay", 5, 0..10, 1, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.STRAFE), ::autoJump))
    
    private val velocityBoost by setting("Velocity Boost", 0.0, 0.0..2.0, 0.01, LambdaUtilsKt.atValue(page, Page.BOOST))
    private val minBoostSpeed by setting("Min Boost Speed", 0.2, 0.0..1.0, 0.01, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    private val maxBoostSpeed by setting("Max Boost Speed", 0.6, 0.0..1.0, 0.01, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    private val maxYSpeed by setting("Max Y Speed", 0.5, 0.0..1.0, 0.01, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    private val boostDelay by setting("Boost Delay", 250, 0..5000, 50, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    private val boostTimeout by setting("Boost Timeout", 500, 0..2500, 50, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    private val boostDecay by setting("Boost Decay", 0.98, 0.1..1.0, 0.001, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    private val boostRange by setting("Boost Range", 4.0f, 1.0f..8.0f, 0.1f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.BOOST)) { velocityBoost > 0.0 })
    
    private val wallCheck by setting("Wall Check", false, LambdaUtilsKt.atValue(page, Page.MISC))
    private val stepPause by setting("Step Pause", 4, 0..20, 1, LambdaUtilsKt.atValue(page, Page.MISC))
    private val reverseStepPause by setting("Reverse Step Pause", 0, 0..10, 1, LambdaUtilsKt.atValue(page, Page.MISC))
    private val holeCheck0 by setting("Hole Check", true, LambdaUtilsKt.atValue(page, Page.MISC))
    private val hRange by setting("H Range", 0.15f, 0.0f..2.0f, 0.05f, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.MISC), ::holeCheck0))
    private val predictTicks by setting("Predict Ticks", 4, 0..10, 1, LambdaUtilsKt.and(LambdaUtilsKt.atValue(page, Page.MISC), ::holeCheck0))

    private val boostTimer = TickTimer()
    private var state = State.AIR
    private var moveSpeed = 0.2873
    private var prevPos: Vec2d? = null
    private var lastDist = 0.0
    private var strafeTicks = 34
    private var burrowTicks = 34
    private var jumpTicks = 34
    private var stepTicks = 34
    private var reverseStepPauseTicks = 34
    private var stepPauseTicks = 34
    private var inHoleTicks = 34
    private var rubberBandTicks = 34
    private var prevCollided = false
    private var prevSpeed = 0.0
    private var boostingSpeed = 0.0
    private val boostList = CopyOnWriteArrayList<BoostInfo>()
    private val stepHeights = doubleArrayOf(0.605, 1.005, 1.505, 2.005, 2.505)

    override fun getHudInfo(): String {
        return "%.2f, %.2f, %.2f".format(baseSpeed, velocityBoost, timerBoost)
    }

    private fun SafeClientEvent.handleVelocity(event: PacketEvent.Receive, pos: Vec3d?, speed: Double, motionY: Double) {
        if (velocityBoost == 0.0) return
        if (isBurrowed()) return
        if (Math.abs(motionY) > maxYSpeed) return
        if (pos != null && player.distanceSqTo(pos) > MathUtilKt.getSq(boostRange)) return

        val newSpeed = Math.min(speed * velocityBoost, maxBoostSpeed)
        if (newSpeed >= minBoostSpeed) {
            if (boostTimeout == 0) {
                if (!prevCollided && !player.collidedHorizontally) {
                    synchronized(boostList) {
                        if (newSpeed > boostingSpeed && boostTimer.tickAndReset(0L)) {
                            boostingSpeed = newSpeed
                            boostTimer.reset(boostDelay.toLong())
                            event.cancel()
                        }
                    }
                }
            } else {
                synchronized(boostList) {
                    boostList.add(BoostInfo(pos, newSpeed, System.currentTimeMillis() + boostTimeout))
                    event.cancel()
                }
            }
        }
    }

    private fun SafeClientEvent.shouldStrafe(): Boolean {
        return !player.capabilities.isFlying && !player.isElytraFlying && !mc.gameSettings.keyBindSneak.isKeyDown &&
               !player.isInWater && !player.isInLava && !player.isInWeb && !player.isOnLadder &&
               MovementUtils.isInputting() && !HoleSnap.isActive() && !BaritoneUtils.isPathing()
    }

    private fun SafeClientEvent.updateState(baseSpeed: Double, motionX: Double, motionZ: Double) {
        if (player.onGround) {
            state = State.JUMP
        }
        when (state) {
            State.JUMP -> {
                if (player.onGround) {
                    if (autoJump) {
                        if (jumpTicks >= jumpDelay) {
                            smartJump(motionX, motionZ)
                            state = State.DECAY
                        }
                    } else if (Math.abs(player.motionY - MovementUtils.applyJumpBoostPotionEffects(player as EntityLivingBase, 0.42)) <= 0.01) {
                        jump()
                        state = State.DECAY
                    }
                }
            }
            State.DECAY -> {
                val decayFactor = if (bbtt) Math.max(jumpDecay, 0.795f) else jumpDecay
                val jumpBoostDecay = decayFactor * (lastDist - baseSpeed)
                moveSpeed = lastDist - jumpBoostDecay
                state = State.AIR
            }
            State.AIR -> {
                var decayFactor = airDecay
                if (decayFactor == 0.9937) {
                    decayFactor = 0.9937106918238994
                }
                moveSpeed = lastDist * decayFactor
            }
        }
    }

    private fun SafeClientEvent.smartJump(motionX: Double, motionZ: Double) {
        val dist = calcBlockDistAhead(motionX * 6.0, motionZ * 6.0)
        val stepHeight = calcStepHeight(dist, motionX, motionZ)
        val multiplier = MovementUtils.getSpeedEffectMultiplier(player as EntityLivingBase)
        if (wallCheck && dist < 3.0 * multiplier && stepHeight > 1.114514) return
        if (dist < 1.4 * multiplier && Step.isActive() && Step.isValidHeight(stepHeight)) return
        if (stepPauseTicks < stepPause || reverseStepPauseTicks < reverseStepPause) return
        if (!holeCheck(motionX * multiplier, motionZ * multiplier, dist)) return
        jump()
    }

    private fun SafeClientEvent.holeCheck(motionX: Double, motionZ: Double, dist: Double): Boolean {
        if (!holeCheck0 || inHoleTicks <= 20 || player.movementInput.jump) return true
        val speed = moveSpeed * predictTicks
        val start = player.positionVector
        val end = start.add(motionX * speed, 0.0, motionZ * speed)
        for (holeInfo in HoleManager.holeInfos) {
            if (holeInfo.origin.y.toDouble() >= player.posY || Math.hypot(holeInfo.center.x - player.posX, holeInfo.center.z - player.posZ) > dist) continue
            val box = toDetectBox(holeInfo.boundingBox, player.posY)
            if (box.contains(start)) return false
            if (predictTicks == 0 || box.contains(end) || box.calculateIntercept(start, end) != null) return false
        }
        return true
    }

    private fun toDetectBox(boundingBox: AxisAlignedBB, playerY: Double): AxisAlignedBB {
        return AxisAlignedBB(boundingBox.minX - hRange, boundingBox.minY, boundingBox.minZ - hRange, boundingBox.maxX + hRange, Math.max(boundingBox.maxY, playerY), boundingBox.maxZ + hRange)
    }

    private fun SafeClientEvent.calcBlockDistAhead(offsetX: Double, offsetZ: Double): Double {
        if (player.collidedHorizontally) return 0.0
        val box = player.entityBoundingBox
        val x = if (offsetX > 0.0) box.maxX else box.minX
        val z = if (offsetZ > 0.0) box.maxZ else box.minZ
        return Math.min(rayTraceDist(Vec3d(x, box.minY + 0.6, z), offsetX, offsetZ), rayTraceDist(Vec3d(x, box.maxY + 0.6, z), offsetX, offsetZ))
    }

    private fun SafeClientEvent.rayTraceDist(start: Vec3d, offsetX: Double, offsetZ: Double): Double {
        val rayTraceResult = world.rayTraceBlocks(start, start.add(offsetX, 0.0, offsetZ), false, true, false)
        return rayTraceResult?.hitVec?.let {
            val x = start.x - it.x
            val z = start.z - it.z
            Math.sqrt(x * x + z * z)
        } ?: 999.0
    }

    private fun SafeClientEvent.jump() {
        player.motionY = calcJumpMotion()
        player.isAirBorne = true
        player.stepHeight = 0.0f
        jumpTicks = 0
        moveSpeed = if (boostingSpeed > 0.1 || rubberBandTicks <= 2 || boostList.isNotEmpty()) {
            moveSpeed * 1.2
        } else {
            Math.min(moveSpeed * jumpBoost, MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, maxJumpSpeed))
        }
    }

    private fun SafeClientEvent.calcJumpMotion(): Double {
        val motion = if (isBurrowed()) 0.42 else (if (jumpMotion == 0.4) 0.3994 else jumpMotion)
        return MovementUtils.applyJumpBoostPotionEffects(player as EntityLivingBase, motion)
    }

    private fun SafeClientEvent.updateFinalSpeed(baseSpeed: Double) {
        if (!isBurrowed()) {
            moveSpeed = Math.max(moveSpeed, baseSpeed)
            if (prevCollided && stepTicks < stepTimeout && stepPauseTicks > stepPause && reverseStepPauseTicks > reverseStepPause && rubberBandTicks > 5) {
                val stepSpeed = Math.min(prevSpeed, MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, maxStepSpeed))
                moveSpeed = Math.max(moveSpeed, stepSpeed)
                if (!player.collidedHorizontally) {
                    prevSpeed = 0.0
                }
            }
        } else {
            moveSpeed = baseSpeed
        }
        moveSpeed = Math.min(moveSpeed, MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, maxSpeed))
    }

    private fun SafeClientEvent.applyVelocityBoost() {
        if (isBurrowed()) {
            resetBoost()
        } else {
            val removeTime = System.currentTimeMillis()
            boostList.removeIf { it.time < removeTime || it.speed < 0.1 }
            if (jumpTicks != 0 && boostTimer.tick(boostDelay.toLong())) {
                val rangeSq = MathUtilKt.getSq(boostRange)
                synchronized(boostList) {
                    val boostInfo = boostList.asSequence()
                        .filter { it.speed > boostingSpeed }
                        .filter { it.pos == null || player.distanceSqTo(it.pos) <= rangeSq }
                        .maxByOrNull { it.speed }
                    
                    boostInfo?.let {
                        boostingSpeed = it.speed
                        boostTimer.reset(boostDelay.toLong())
                        resetBoost()
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.updateVelocityBoost() {
        val decayFactor = boostDecay
        val blockFactor = if (player.onGround) {
            val blockPos = BlockPos(player.posX, player.entityBoundingBox.minY - 1.0, player.posZ)
            val blockState = world.getBlockState(blockPos)
            blockState.block.getSlipperiness(blockState, world, blockPos, player) * 0.91
        } else {
            0.91
        }
        val decay = decayFactor * blockFactor
        synchronized(boostList) {
            boostList.forEach { it.speed *= decay }
        }
        boostingSpeed = if (player.collidedHorizontally) 0.0 else boostingSpeed * decay
    }

    private fun SafeClientEvent.calcStepHeight(dist: Double, motionX: Double, motionZ: Double): Double {
        val pos = EntityUtils.getBetterPosition(player)
        if (world.getBlockState(pos).getCollisionBoundingBox(world, pos) != null) return 0.0
        val i = Math.max(Math.round(dist).toInt(), 1)
        var minStepHeight = Double.MAX_VALUE
        val x = motionX * i
        val z = motionZ * i
        minStepHeight = checkBox(minStepHeight, x, 0.0)
        minStepHeight = checkBox(minStepHeight, 0.0, z)
        return if (minStepHeight == Double.MAX_VALUE) 0.0 else minStepHeight
    }

    private fun SafeClientEvent.checkBox(minStepHeight: Double, offsetX: Double, offsetZ: Double): Double {
        val box = player.entityBoundingBox.offset(offsetX, 0.0, offsetZ)
        if (!world.collidesWithAnyBlock(box)) return minStepHeight
        var stepHeight = minStepHeight
        for (y in stepHeights) {
            if (y > minStepHeight) break
            val stepBox = AxisAlignedBB(box.minX, box.minY + y - 0.5, box.minZ, box.maxX, box.minY + y, box.maxZ)
            val boxList = world.getCollisionBoxes(null, stepBox)
            val maxHeight = boxList.maxOfOrNull { it.maxY } ?: continue
            val maxStepHeight = maxHeight - player.posY
            if (world.collidesWithAnyBlock(box.offset(0.0, maxStepHeight, 0.0))) continue
            stepHeight = maxStepHeight
            break
        }
        return stepHeight
    }

    fun reset() {
        TimerManager.resetTimer(this)
        state = State.AIR
        val player = SafeClientEvent.instance?.player as? EntityPlayerSP
        moveSpeed = if (player != null) MovementUtils.applySpeedPotionEffects(player, baseSpeed) else baseSpeed
        prevPos = null
        lastDist = 0.0
        prevCollided = false
        prevSpeed = 0.0
        boostingSpeed = 0.0
    }

    private fun resetBoost() {
        boostList.clear()
    }

    private fun isBurrowed() = burrowTicks < 10

    fun resetReverseStep() {
        stepPauseTicks = 0
    }

    private fun globalCheck() = PacketFlyOld.isEnabled

    init {
        onEnable {
            reset()
            resetBoost()
            strafeTicks = 34
            burrowTicks = 34
            jumpTicks = 34
            stepTicks = 34
            reverseStepPauseTicks = 34
            stepPauseTicks = 34
            inHoleTicks = 34
            rubberBandTicks = 34
        }

        listener<PacketEvent.Receive> {
            if (globalCheck()) return@listener
            val packet = it.packet
            if (packet is SPacketPlayerPosLook) {
                rubberBandTicks = 0
                reset()
                resetBoost()
                boostTimer.reset(1000L)
            } else if (packet is SPacketExplosion) {
                handleVelocity(it, Vec3d(packet.posX, packet.posY, packet.posZ), Math.hypot(packet.motionX.toDouble(), packet.motionZ.toDouble()), packet.motionY.toDouble())
            } else if (packet is SPacketEntityVelocity && packet.entityID == player.entityId) {
                handleVelocity(it, null, Math.hypot(packet.motionX / 8000.0, packet.motionZ / 8000.0) - moveSpeed, packet.motionY / 8000.0)
            }
        }

        listener<TickEvent.Post> {
            if (globalCheck()) return@listener
            strafeTicks++
            burrowTicks++
            jumpTicks++
            stepTicks++
            reverseStepPauseTicks++
            stepPauseTicks++
            inHoleTicks++
            rubberBandTicks++
        }

        listener<InputUpdateEvent> {
            if (globalCheck()) return@listener
            if (it.movementInput is MovementInputFromOptions && autoJump && shouldStrafe()) {
                it.movementInput.jump = false
            }
        }

        listener<PlayerTravelEvent> {
            if (globalCheck()) return@listener
            if (Burrow.isBurrowed(player)) {
                burrowTicks = 0
            }
            if (!shouldStrafe()) {
                reset()
                return@listener
            }
            if (!isBurrowed()) {
                TimerManager.modifyTimer(this, 50.0f / timerBoost)
            }
            strafeTicks = 0
        }

        listener<PlayerMoveEvent.Pre> { event ->
            if (globalCheck()) return@listener
            if (shouldStrafe()) {
                val yaw = MovementUtils.calcMoveYaw(player)
                val dirX = -Math.sin(yaw)
                val dirZ = Math.cos(yaw)
                val baseSpeedVal = MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, baseSpeed)
                updateState(baseSpeedVal, dirX, dirZ)
                updateFinalSpeed(baseSpeedVal)
                if (boostTimeout > 0) {
                    applyVelocityBoost()
                }
                val boostedSpeed = Math.min(moveSpeed + boostingSpeed, MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, maxSpeed))
                player.motionX = dirX * baseSpeedVal
                player.motionZ = dirZ * baseSpeedVal
                event.x = dirX * boostedSpeed
                event.z = dirZ * boostedSpeed
                if (!prevCollided && !player.collidedHorizontally && jumpTicks > 2) {
                    prevSpeed = moveSpeed
                    stepTicks = 0
                }
                prevPos = Vec2d(player.posX, player.posZ)
                prevCollided = player.collidedHorizontally
            } else if (strafeTicks <= 1) {
                player.motionX = 0.0
                player.motionZ = 0.0
                event.x = 0.0
                event.z = 0.0
            }
        }

        listener<PlayerMoveEvent.Post> {
            if (globalCheck()) return@listener
            if (jumpTicks == 0) {
                player.stepHeight = 0.6f
            }
            prevPos?.let {
                lastDist = Math.hypot(it.x - player.posX, it.y - player.posZ)
            }
            var decayFactor = airDecay
            if (decayFactor == 0.9937) {
                decayFactor = 0.9937106918238994
            }
            prevSpeed *= decayFactor
            updateVelocityBoost()
        }

        listener<StepEvent> {
            if (globalCheck()) return@listener
            stepPauseTicks = 0
            prevSpeed = 0.0
        }
    }

    private class BoostInfo(val pos: Vec3d?, var speed: Double, val time: Long)

    private enum class Page {
        STRAFE, BOOST, MISC
    }

    private enum class State {
        JUMP, DECAY, AIR
    }
}
