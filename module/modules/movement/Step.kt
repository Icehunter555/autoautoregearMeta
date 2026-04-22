package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.StepEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.exploit.Clip
import dev.wizard.meta.module.modules.exploit.PacketFlyOld
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import net.minecraft.block.*
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB

object Step : Module(
    name = "Step",
    category = Category.MOVEMENT,
    description = "Changes the vanilla behavior for stepping up blocks",
    priority = 200
) {
    var mode by setting("Mode", Mode.PACKET)
    private val entityStep by setting("Entity Step", true)
    var useTimer by setting("Use Timer", false, { mode == Mode.PACKET })
    var nobedstep by setting("No Bed Step", false)
    private val strictYMotion by setting("Strict Y Motion", false)
    private val autoDisable by setting("Auto Disable", false)
    var minHeight by setting("Min Height", 0.9f, 0.6f..2.5f, 0.1f)
    var maxHeight by setting("Max Height", 2.0f, 0.6f..2.5f, 0.1f)
    private val enableTicks by setting("Enable Ticks", 0, 0..50, 1)
    var postTimer by setting("Post Timer", 0.8f, 0.01f..1.0f, 0.01f)
    private val maxPostTicks by setting("Max Post Ticks", 40, 0..100, 1)

    const val DEFAULT_HEIGHT = 0.6f
    private var timeoutTick = Int.MIN_VALUE
    private var shouldDisable = false
    private var prevCollided = false
    private var collideTicks = 0

    private val step10 = doubleArrayOf(0.42499, 0.75752)
    private val step12 = doubleArrayOf(0.42499, 0.82721, 1.13981)
    private val step13 = doubleArrayOf(0.42499, 0.82108, 1.13367, 1.32728)
    private val step15 = doubleArrayOf(0.42499, 0.76, 1.01, 1.093, 1.015)
    private val step20 = doubleArrayOf(0.42499, 0.78, 0.63, 0.51, 0.9, 1.21, 1.45, 1.43)
    private val step25 = doubleArrayOf(0.42499, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.907)

    override fun getHudInfo(): String {
        return "$minHeight - $maxHeight"
    }

    override fun isActive(): Boolean {
        if (!isEnabled) return false
        val event = SafeClientEvent.instance ?: return false
        return shouldRunStep(event, event.player.motionX, event.player.motionY, event.player.motionZ)
    }

    private fun shouldRunStep(event: SafeClientEvent, x: Double, y: Double, z: Double): Boolean {
        if (event.mc.gameSettings.keyBindSneak.isKeyDown) return false
        if (EntityUtils.isFlying(event.player)) return false
        if (event.player.isOnLadder) return false
        if (EntityUtils.isInOrAboveLiquid(event.player)) return false
        if (mode != Mode.VANILLA) {
            if (TimerManager.globalTicks <= timeoutTick) return false
        }
        if (strictYMotion) {
            if (y < -0.08 || y > 0.0) return false
            if (event.player.prevPosY != event.player.posY) return false
        }
        if (!MovementUtils.isInputting() && !HolePathFinder.isActive()) return false
        if (x * x + z * z <= 0.001) return false
        return true
    }

    private fun vanillaStep(event: SafeClientEvent) {
        event.player.stepHeight = maxHeight
        StepEvent.post()
        shouldDisable = autoDisable
    }

    private fun packetStep(event: SafeClientEvent, stepHeight: Double) {
        val array = getStepArray(stepHeight)
        if (array != null) {
            for (offset in array) {
                event.connection.sendPacket(CPacketPlayer.Position(event.player.posX, event.player.posY + offset, event.player.posZ, false))
            }
            event.player.stepHeight = maxHeight
            StepEvent.post()
            shouldDisable = autoDisable
            if (useTimer) {
                val targetTimer = 1.09f
                val extraPackets = array.size + 1
                val ticks = Math.min(MathUtilKt.ceilToInt(extraPackets / (targetTimer - postTimer)), maxPostTicks)
                val adjustedTimer = Math.max((targetTimer * ticks - extraPackets) / ticks, postTimer)
                timeoutTick = TimerManager.globalTicks + ticks - 1
                TimerManager.modifyTimer(this, 50.0f / adjustedTimer, ticks)
            }
        }
    }

    private fun calcStepHeight(event: SafeClientEvent, moveEvent: PlayerMoveEvent.Pre): Double {
        val playerBox = event.player.entityBoundingBox
        var motionX = moveEvent.x
        var motionY = moveEvent.y
        var motionZ = moveEvent.z
        val x1 = motionX
        val y1 = motionY
        val z1 = motionZ

        while (motionX != 0.0 && event.world.getCollisionBoxes(event.player, playerBox.offset(motionX, -maxHeight.toDouble(), 0.0)).isEmpty()) {
            if (motionX < 0.05 && motionX >= -0.05) motionX = 0.0 else if (motionX > 0.0) motionX -= 0.05 else motionX += 0.05
        }
        while (motionZ != 0.0 && event.world.getCollisionBoxes(event.player, playerBox.offset(0.0, -maxHeight.toDouble(), motionZ)).isEmpty()) {
            if (motionZ < 0.05 && motionZ >= -0.05) motionZ = 0.0 else if (motionZ > 0.0) motionZ -= 0.05 else motionZ += 0.05
        }
        while (motionX != 0.0 && motionZ != 0.0 && event.world.getCollisionBoxes(event.player, playerBox.offset(motionX, -maxHeight.toDouble(), motionZ)).isEmpty()) {
            if (motionX < 0.05 && motionX >= -0.05) motionX = 0.0 else if (motionX > 0.0) motionX -= 0.05 else motionX += 0.05
            if (motionZ < 0.05 && motionZ >= -0.05) motionZ = 0.0 else if (motionZ > 0.0) motionZ -= 0.05 else motionZ += 0.05
        }

        val list1 = event.world.getCollisionBoxes(event.player, playerBox.expand(motionX, motionY, motionZ))
        var currentBox = playerBox
        var currentY = motionY
        if (currentY != 0.0) {
            for (box in list1) {
                currentY = box.calculateYOffset(playerBox, currentY)
            }
            currentBox = currentBox.offset(0.0, currentY, 0.0)
        }
        var currentX = motionX
        if (currentX != 0.0) {
            for (box in list1) {
                currentX = box.calculateXOffset(currentBox, currentX)
            }
            if (currentX != 0.0) {
                currentBox = currentBox.offset(currentX, 0.0, 0.0)
            }
        }
        var currentZ = motionZ
        if (currentZ != 0.0) {
            for (box in list1) {
                currentZ = box.calculateZOffset(currentBox, currentZ)
            }
        }

        val flag = event.player.onGround || (y1 != currentY && y1 < 0.0)
        if (flag && (x1 != currentX || z1 != currentZ)) {
            val x2 = currentX
            val z2 = currentZ
            var box3 = playerBox
            val yStep = maxHeight.toDouble()
            val list2 = event.world.getCollisionBoxes(event.player, playerBox.expand(x1, yStep, z1))
            var box4 = box3.expand(x1, 0.0, z1)
            var y3 = yStep
            for (box in list2) {
                y3 = box.calculateYOffset(box4, y3)
            }
            box3 = box3.offset(0.0, y3, 0.0)
            var x3 = x1
            for (box in list2) {
                x3 = box.calculateXOffset(box3, x3)
            }
            box3 = box3.offset(x3, 0.0, 0.0)
            var z3 = z1
            for (box in list2) {
                z3 = box.calculateZOffset(box3, z3)
            }
            box3 = box3.offset(0.0, 0.0, z3)

            var box5 = playerBox
            var y4 = yStep
            for (box in list2) {
                y4 = box.calculateYOffset(box5, y4)
            }
            box5 = box5.offset(0.0, y4, 0.0)
            var x4 = x1
            for (box in list2) {
                x4 = box.calculateXOffset(box5, x4)
            }
            box5 = box5.offset(x4, 0.0, 0.0)
            var z4 = z1
            for (box in list2) {
                z4 = box.calculateZOffset(box5, z4)
            }
            box5 = box5.offset(0.0, 0.0, z4)

            val speed3 = x3 * x3 + z3 * z3
            val speed4 = x4 * x4 + z4 * z4
            var finalBox: AxisAlignedBB
            var finalX: Double
            var finalZ: Double
            var finalY: Double
            if (speed3 > speed4) {
                finalX = x3
                finalZ = z3
                finalY = -y3
                finalBox = box3
            } else {
                finalX = x4
                finalZ = z4
                finalY = -y4
                finalBox = box5
            }
            
            var yFinal = finalY
            for (box in list2) {
                yFinal = box.calculateYOffset(finalBox, yFinal)
            }
            finalBox = finalBox.offset(0.0, yFinal, 0.0)

            if (x2 * x2 + z2 * z2 < finalX * finalX + finalZ * finalZ) {
                return finalBox.minY - event.player.posY
            }
        }
        return 0.0
    }

    private fun getStepArray(stepHeight: Double): DoubleArray? {
        return when {
            stepHeight in 0.6..1.05 -> {
                step10[0] = 0.42499 * stepHeight
                step10[1] = 0.75752 * stepHeight
                step10
            }
            stepHeight in 1.05..1.2 -> step12
            stepHeight in 1.2..1.3 -> step13
            stepHeight in 1.3..1.5 -> step15
            stepHeight in 1.5..2.0 -> step20
            stepHeight in 2.0..2.5 -> step25
            else -> null
        }
    }

    fun isValidHeight(height: Double): Boolean {
        return height in minHeight.toDouble()..maxHeight.toDouble()
    }

    private fun globalCheck(): Boolean {
        return PacketFlyOld.isEnabled || (nobedstep && checkBadBlock()) || Clip.isActive()
    }

    fun checkBadBlock(): Boolean {
        val player = mc.player ?: return false
        val block = mc.world.getBlockState(EntityUtils.getBetterPosition(player)).block
        return block is BlockBed || block is BlockSlab || block is BlockPistonBase || block is BlockPistonMoving || block is BlockPistonExtension || block is BlockWeb
    }

    init {
        onDisable {
            mc.player?.let {
                it.ridingEntity?.stepHeight = 1.0f
                it.stepHeight = 0.6f
            }
            timeoutTick = Int.MIN_VALUE
            shouldDisable = false
            prevCollided = false
            collideTicks = 0
        }

        onToggle {
            BaritoneUtils.settings?.assumeStep?.value = it
        }

        listener<PlayerMoveEvent.Pre>(-100, true) { event ->
            if (globalCheck()) return@listener
            val flag = shouldRunStep(this, event.x, event.y, event.z)
            if (!isEnabled || (HolePathFinder.enableStep && HolePathFinder.isActive())) {
                if (enableTicks > 0) {
                    val collided = player.collidedHorizontally
                    if (flag && (prevCollided || collided)) {
                        collideTicks++
                    } else {
                        collideTicks = 0
                    }
                    prevCollided = collided
                }
                if (enableTicks == 0 || collideTicks <= enableTicks) {
                    return@listener
                }
            }
            
            player.ridingEntity?.let {
                it.stepHeight = if (entityStep && flag) maxHeight else 1.0f
            }

            if (flag && !player.isRiding && player.onGround) {
                val stepHeight = calcStepHeight(this, event)
                if (!stepHeight.isNaN() && (HolePathFinder.isActive() || stepHeight >= minHeight) && stepHeight <= maxHeight) {
                    val disabled = isDisabled
                    enable()
                    when (mode) {
                        Mode.VANILLA -> vanillaStep(this)
                        Mode.PACKET -> packetStep(this, stepHeight)
                    }
                    shouldDisable = shouldDisable || disabled
                }
            }
        }

        listener<PlayerMoveEvent.Post> {
            if (globalCheck()) return@listener
            player.stepHeight = 0.6f
            if (shouldDisable) {
                disable()
            }
        }
    }

    enum class Mode {
        VANILLA, PACKET
    }
}
