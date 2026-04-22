package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.KillAura
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import net.minecraft.util.math.Vec3d

object TargetStrafe : Module(
    name = "TargetStrafe",
    category = Category.MOVEMENT,
    description = "Strafes around a target in various patterns",
    priority = 1010
) {
    private val mode by setting("Mode", StrafeMode.ADAPTIVE)
    private val autoJump by setting("Auto Jump", true)
    private val useStep by setting("Use Step", true)
    private val stepHeight by setting("Step Height", 1.0f, 0.6f..2.5f, 0.1f, { useStep })
    private val minDistance by setting("Min Distance", 2.0f, 0.5f..6.0f, 0.1f)
    private val maxDistance by setting("Max Distance", 3.5f, 0.5f..10.0f, 0.1f)
    private val targetLostDistance by setting("Target Lost Distance", 15.0f, 5.0f..32.0f, 0.5f)
    private val hSpeed by setting("Speed", 0.2873f, 0.001f..1.0f, 0.001f)
    private val needsAura by setting("Needs Aura", true)
    private val antiStuck by setting("Anti Stuck", true)
    private val adaptiveSpeed by setting("Adaptive Speed", true, { mode == StrafeMode.ADAPTIVE })
    private val orbitSmoothing by setting("Orbit Smoothing", 0.3f, 0.0f..1.0f, 0.05f, { mode == StrafeMode.ADAPTIVE })
    private val distanceCorrection by setting("Distance Correction", 0.5f, 0.0f..1.0f, 0.05f, { mode == StrafeMode.ADAPTIVE })
    private val predictionTicks by setting("Prediction Ticks", 3, 0..10, 1, { mode == StrafeMode.ADAPTIVE })
    private val zigzagAngle by setting("Zigzag Angle", 45.0f, 15.0f..75.0f, 5.0f, { mode == StrafeMode.ZIGZAG })
    private val zigzagDuration by setting("Zigzag Duration", 20, 5..60, 1, { mode == StrafeMode.ZIGZAG })

    private var direction = 1
    private var currentDistance = 0.0
    private var strafing = false
    private var zigzagTimer = 0
    private var targetLastPos: Vec3d? = null
    private var targetVelocity: Vec3d? = null
    private var smoothedAngle = 0.0f
    private var collideCount = 0

    override fun getHudInfo(): String {
        val targetName = CombatManager.target?.name ?: "NONE"
        return "${mode.displayName}, D: %.1f, $targetName".format(currentDistance)
    }

    private fun SafeClientEvent.doStrafeAtSpeed(event: PlayerMoveEvent.Pre, rotation: Float, target: Vec3d) {
        var playerSpeed = hSpeed
        val disX = player.posX - target.x
        val disZ = player.posZ - target.z
        currentDistance = Math.sqrt(disX * disX + disZ * disZ)
        
        player.getActivePotionEffect(MobEffects.SPEED)?.let {
            playerSpeed *= 1.0f + 0.2f * (it.amplifier + 1)
        }

        if (adaptiveSpeed && mode == StrafeMode.ADAPTIVE) {
            val distanceError = Math.abs(currentDistance - (minDistance + maxDistance) / 2.0)
            val speedMultiplier = 1.0f + (distanceError / maxDistance.toDouble()).toFloat().coerceIn(0.0f, 0.5f)
            playerSpeed *= speedMultiplier
        }

        val rotationYaw = when (mode) {
            StrafeMode.ADAPTIVE -> calculateAdaptiveStrafe(rotation, currentDistance, target)
            StrafeMode.CIRCLE -> calculateCircleStrafe(rotation, currentDistance)
            StrafeMode.ZIGZAG -> calculateZigzagStrafe(rotation, currentDistance)
            StrafeMode.INFINITY -> calculateInfinityStrafe(rotation, currentDistance)
        }

        smoothedAngle = if (mode == StrafeMode.ADAPTIVE) {
            smoothedAngle + (rotationYaw - smoothedAngle) * (1.0f - orbitSmoothing)
        } else {
            rotationYaw
        }

        val radians = Math.toRadians((smoothedAngle + 90.0f).toDouble())
        event.x = playerSpeed.toDouble() * Math.cos(radians)
        event.z = playerSpeed.toDouble() * Math.sin(radians)

        if (autoJump && player.onGround) {
            player.jump()
        }
    }

    private fun SafeClientEvent.calculateAdaptiveStrafe(rotation: Float, distance: Double, target: Vec3d): Float {
        val preferredDistance = (minDistance + maxDistance) / 2.0
        val distanceError = distance - preferredDistance
        val orbitAngle = rotation + 90.0f * direction
        
        var adjustedTarget = target
        targetVelocity?.let { velocity ->
            if (predictionTicks > 0 && (velocity.x != 0.0 || velocity.z != 0.0)) {
                adjustedTarget = target.add(velocity.scale(predictionTicks.toDouble()))
            }
        }

        val toTargetAngle = Math.toDegrees(Math.atan2(adjustedTarget.z - player.posZ, adjustedTarget.x - player.posX)).toFloat() - 90.0f
        val correctionStrength = distanceCorrection * 90.0f

        val distanceCorrectionAngle = when {
            distance > maxDistance -> toTargetAngle
            distance < minDistance -> toTargetAngle + 180.0f
            else -> {
                val correctionAmount = (distanceError / preferredDistance).toFloat() * correctionStrength
                orbitAngle - correctionAmount * direction
            }
        }

        val blendFactor = when {
            distance > maxDistance + 0.5 -> 1.0f
            distance < minDistance - 0.5 -> 1.0f
            else -> Math.abs(distanceError / preferredDistance).toFloat().coerceIn(0.0f, 1.0f)
        }

        return lerpAngle(orbitAngle, distanceCorrectionAngle, blendFactor)
    }

    private fun calculateCircleStrafe(rotation: Float, distance: Double): Float {
        val preferredDistance = (minDistance + maxDistance) / 2.0
        var rotationYaw = rotation + 90.0f * direction
        when {
            distance > maxDistance -> rotationYaw = rotation
            distance < minDistance -> rotationYaw = rotation + 180.0f
            distance > preferredDistance + 0.3 -> rotationYaw -= 15.0f * direction
            distance < preferredDistance - 0.3 -> rotationYaw += 15.0f * direction
        }
        return rotationYaw
    }

    private fun calculateZigzagStrafe(rotation: Float, distance: Double): Float {
        if (++zigzagTimer >= zigzagDuration) {
            direction = -direction
            zigzagTimer = 0
        }
        var rotationYaw = rotation + zigzagAngle * direction
        when {
            distance > maxDistance -> rotationYaw = rotation
            distance < minDistance -> rotationYaw = rotation + 180.0f
        }
        return rotationYaw
    }

    private fun calculateInfinityStrafe(rotation: Float, distance: Double): Float {
        val time = System.currentTimeMillis() / 1000.0
        val wave = Math.sin(time * 2.0).toFloat()
        val preferredDistance = (minDistance + maxDistance) / 2.0
        var rotationYaw = rotation + 90.0f * wave
        when {
            distance > maxDistance -> rotationYaw = rotation
            distance < minDistance -> rotationYaw = rotation + 180.0f
            distance > preferredDistance + 0.3 -> rotationYaw -= 15.0f
            distance < preferredDistance - 0.3 -> rotationYaw += 15.0f
        }
        return rotationYaw
    }

    private fun SafeClientEvent.canStrafe(): Boolean {
        val target = CombatManager.target ?: return false
        if (player.getDistance(target) > targetLostDistance) return false
        return !needsAura || KillAura.isEnabled
    }

    private fun reset() {
        zigzagTimer = 0
        targetLastPos = null
        targetVelocity = null
        smoothedAngle = 0.0f
        collideCount = 0
    }

    private fun lerpAngle(from: Float, to: Float, t: Float): Float {
        var delta = to - from
        while (delta > 180.0f) delta -= 360.0f
        while (delta < -180.0f) delta += 360.0f
        return from + delta * t
    }

    init {
        onDisable {
            reset()
            mc.player?.stepHeight = 0.6f
        }

        listener<PlayerMoveEvent.Pre> { event ->
            if (useStep && strafing) {
                player.stepHeight = stepHeight
            } else {
                player.stepHeight = 0.6f
            }

            if (player.collidedHorizontally && antiStuck) {
                if (++collideCount > 3) {
                    direction = -direction
                    collideCount = 0
                }
            } else {
                collideCount = 0
            }

            if (canStrafe()) {
                val target = CombatManager.target!!
                val rotations = RotationUtils.getRotationToEntity(this, target)
                targetLastPos?.let {
                    targetVelocity = target.positionVector.subtract(it)
                }
                targetLastPos = target.positionVector
                doStrafeAtSpeed(event, Vec2f.getX(rotations), target.positionVector)
                strafing = true
            } else {
                strafing = false
                reset()
            }
        }

        listener<PlayerMoveEvent.Post> {
            if (!strafing || !useStep) {
                player.stepHeight = 0.6f
            }
        }
    }

    private enum class StrafeMode(override val displayName: CharSequence) : DisplayEnum {
        ADAPTIVE("Adaptive"),
        CIRCLE("Circle"),
        ZIGZAG("Zigzag"),
        INFINITY("Infinity")
    }
}
