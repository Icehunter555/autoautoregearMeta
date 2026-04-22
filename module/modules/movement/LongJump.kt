package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.OnUpdateWalkingPlayerEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.init.MobEffects
import net.minecraft.network.play.server.SPacketPlayerPosLook
import java.math.BigDecimal
import java.math.RoundingMode

object LongJump : Module(
    name = "LongJump",
    category = Category.MOVEMENT,
    description = "jump long",
    priority = 1010
) {
    private val mode by setting("Mode", Mode.BYPASS)
    private val speed by setting("Speed", 4.5f, 0.5f..20.0f, 0.1f)
    private val modifier by setting("Modifier", 5.0f, 0.1f..10.0f, 0.1f)
    private val glide by setting("Glide", 1.0f, 0.1f..10.0f, 0.1f)
    private val shortJump by setting("ShortJump", false)
    private val groundCheck by setting("GroundCheck", GroundCheck.NORMAL)
    private val autoDisable by setting("AutoDisable", true)

    private val timer = TickTimer(TimeUnit.MILLISECONDS)
    private var timerStatus = true
    private var walkingStatus = false
    private var onGroundTracker = 0
    private var walkingState = 0.0
    private var totalWalkingState = 0.0
    private var bypassState = 1
    private var state = 1
    private var currentSpeed = 0.2873
    private var groundTracker = false

    override fun getHudInfo(): String {
        val mde = when (mode) {
            Mode.NORMAL -> "Norm"
            Mode.BYPASS -> "Byp"
        }
        return "$mde, $speed, $modifier"
    }

    private fun SafeClientEvent.handleNormalMode(event: PlayerMoveEvent.Pre) {
        if (player.moveStrafing <= 0.0f && player.moveForward <= 0.0f) {
            state = 1
        }
        if (roundDecimalUp(player.posY - Math.floor(player.posY), 3) == 0.943) {
            player.motionY -= 0.0157 * glide
            event.y -= 0.0157 * glide
        }
        when (state) {
            1 -> {
                if (player.moveForward != 0.0f || player.moveStrafing != 0.0f) {
                    state = 2
                    currentSpeed = speed.toDouble() * getBaseSpeed() - 0.01
                }
            }
            2 -> {
                player.motionY = 0.0848 * modifier
                event.y = 0.0848 * modifier
                state = 3
                currentSpeed *= 2.149802
            }
            3 -> {
                state = 4
                walkingState = 0.66 * totalWalkingState
                currentSpeed = totalWalkingState - walkingState
            }
            else -> {
                val boxes = world.getCollisionBoxes(player, player.entityBoundingBox.offset(0.0, player.motionY, 0.0))
                if (boxes.isNotEmpty() || player.collidedVertically) {
                    state = 1
                }
                currentSpeed = totalWalkingState - totalWalkingState / 159.0
            }
        }
        currentSpeed = Math.max(currentSpeed, getBaseSpeed())
        applyMovement(event)
    }

    private fun SafeClientEvent.handleBypassMode(event: PlayerMoveEvent.Pre) {
        if (!timerStatus) return
        if (player.onGround) timer.reset()

        if (roundDecimalUp(player.posY - Math.floor(player.posY), 3) == 0.41) {
            player.motionY = 0.0
        }
        if (player.moveStrafing <= 0.0f && player.moveForward <= 0.0f) {
            bypassState = 1
        }
        if (roundDecimalUp(player.posY - Math.floor(player.posY), 3) == 0.943) {
            player.motionY = 0.0
        }
        when (bypassState) {
            1 -> {
                if (player.moveForward != 0.0f || player.moveStrafing != 0.0f) {
                    bypassState = 2
                    currentSpeed = speed.toDouble() * getBaseSpeed() - 0.01
                }
            }
            2 -> {
                bypassState = 3
                if (!shortJump) {
                    player.motionY = 0.424
                }
                event.y = 0.424
                currentSpeed *= 2.149802
            }
            3 -> {
                bypassState = 4
                val spd = 0.66 * (totalWalkingState - getBaseSpeed())
                currentSpeed = totalWalkingState - spd
            }
            else -> {
                val boxes = world.getCollisionBoxes(player, player.entityBoundingBox.offset(0.0, player.motionY, 0.0))
                if (boxes.isNotEmpty() || player.collidedVertically) {
                    bypassState = 1
                }
                currentSpeed = totalWalkingState - totalWalkingState / 159.0
            }
        }
        currentSpeed = Math.max(currentSpeed, getBaseSpeed())
        applyMovement(event)
    }

    private fun SafeClientEvent.applyMovement(event: PlayerMoveEvent.Pre) {
        var moveForward = player.movementInput.moveForward
        var moveStrafe = player.movementInput.moveStrafe
        var rotationYaw = player.rotationYaw
        if (moveForward == 0.0f && moveStrafe == 0.0f) {
            event.x = 0.0
            event.z = 0.0
            return
        }
        if (moveForward != 0.0f) {
            if (moveStrafe >= 1.0f) {
                rotationYaw += if (moveForward > 0.0f) -45.0f else 45.0f
                moveStrafe = 0.0f
            } else if (moveStrafe <= -1.0f) {
                rotationYaw += if (moveForward > 0.0f) 45.0f else -45.0f
                moveStrafe = 0.0f
            }
            moveForward = if (moveForward > 0.0f) 1.0f else -1.0f
        }
        val cos = Math.cos(Math.toRadians((rotationYaw + 90.0f).toDouble()))
        val sin = Math.sin(Math.toRadians((rotationYaw + 90.0f).toDouble()))
        event.x = moveForward.toDouble() * currentSpeed * cos + moveStrafe.toDouble() * currentSpeed * sin
        event.z = moveForward.toDouble() * currentSpeed * sin - moveStrafe.toDouble() * currentSpeed * cos
    }

    private fun roundDecimalUp(d: Double, places: Int): Double {
        require(places >= 0) { "places < 0" }
        return BigDecimal(d).setScale(places, RoundingMode.HALF_UP).toDouble()
    }

    @JvmStatic
    fun getBaseSpeed(): Double {
        var baseSpeed = 0.2873
        mc.player?.let { player ->
            player.getActivePotionEffect(MobEffects.SPEED)?.let {
                val amplifier = it.amplifier
                baseSpeed *= 1.0 + 0.2 * (amplifier + 1)
            }
        }
        return baseSpeed
    }

    init {
        onEnable {
            currentSpeed = getBaseSpeed()
            mc.player?.onGround = true
            groundTracker = groundCheck != GroundCheck.NONE
            walkingStatus = false
            timerStatus = true
            totalWalkingState = 0.0
            state = 1
        }

        onDisable {
            if (mode == Mode.BYPASS) {
                mc.player?.capabilities?.isFlying = false
                mc.player?.onGround = false
            }
        }

        listener<OnUpdateWalkingPlayerEvent.Pre> {
            if (Speed.isEnabled) return@listener
            if (groundTracker) {
                when (groundCheck) {
                    GroundCheck.NORMAL -> {
                        if (player.onGround) groundTracker = false
                    }
                    GroundCheck.EDGE -> {
                        if (player.onGround && !player.isSneaking && world.getCollisionBoxes(player, player.entityBoundingBox.offset(0.0, 0.0, 0.0).grow(0.001)).isEmpty()) {
                            groundTracker = false
                        }
                    }
                    else -> {}
                }
                return@listener
            }

            if (mode == Mode.NORMAL) {
                val difX = player.posX - player.prevPosX
                val difZ = player.posZ - player.prevPosZ
                totalWalkingState = Math.sqrt(difX * difX + difZ * difZ)
            } else {
                val difX = player.posX - player.prevPosX
                val difZ = player.posZ - player.prevPosZ
                totalWalkingState = Math.sqrt(difX * difX + difZ * difZ)
                if (!walkingStatus) return@listener
                player.motionY = 0.005
            }
        }

        listener<PacketEvent.Receive> {
            if (Speed.isEnabled) return@listener
            if (it.packet is SPacketPlayerPosLook && autoDisable) {
                disable()
            }
        }

        listener<PlayerMoveEvent.Pre> { event ->
            if (groundTracker) return@listener
            if (Speed.isEnabled) return@listener
            if (player != mc.renderViewEntity) return@listener

            when (mode) {
                Mode.NORMAL -> handleNormalMode(event)
                Mode.BYPASS -> handleBypassMode(event)
            }

            if (player.onGround) {
                onGroundTracker++
            } else if (!player.onGround && onGroundTracker != 0) {
                onGroundTracker--
            }

            if (shortJump) {
                if (timer.tick(35L)) walkingStatus = true
                if (timer.tick(2490L)) {
                    walkingStatus = false
                    timerStatus = false
                    player.motionX *= 0.0
                    player.motionZ *= 0.0
                }
                if (timer.tick(2820L)) {
                    timerStatus = true
                    player.motionX *= 0.0
                    player.motionZ *= 0.0
                    timer.reset()
                }
            } else {
                if (timer.tick(480L)) {
                    player.motionX *= 0.0
                    player.motionZ *= 0.0
                    timerStatus = false
                }
                if (timer.tick(780L)) {
                    player.motionX *= 0.0
                    player.motionZ *= 0.0
                    timerStatus = true
                    timer.reset()
                }
            }
        }
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        NORMAL("Normal"), BYPASS("Bypass")
    }

    private enum class GroundCheck(override val displayName: CharSequence) : DisplayEnum {
        NONE("None"), NORMAL("Normal"), EDGE("Edge Jump")
    }
}
