package dev.wizard.meta.util

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec3f
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import net.minecraft.util.MovementInput
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.*

object MovementUtils {
    private val mc = Minecraft.getMinecraft()

    fun isInputting(movementInput: MovementInput? = Wrapper.player?.movementInput, jump: Boolean = false, sneak: Boolean = false): Boolean {
        if (movementInput == null) return false
        return movementInput.moveForward != 0.0f || movementInput.moveStrafe != 0.0f || (jump && movementInput.jump) || (sneak && movementInput.sneak)
    }

    fun Entity.isMoving(): Boolean {
        return getSpeed() > 1.0E-4
    }

    fun Entity.getSpeed(): Double {
        return hypot(motionX, motionZ)
    }

    fun Entity.getRealSpeed(): Double {
        return hypot(posX - prevPosX, posZ - prevPosZ)
    }

    fun Entity.getRealMotionX(): Double = posX - prevPosX
    fun Entity.getRealMotionY(): Double = posY - prevPosY
    fun Entity.getRealMotionZ(): Double = posZ - prevPosZ

    fun EntityPlayerSP.calcMoveYaw(): Double {
        return calcMoveYaw(rotationYaw, movementInput.moveForward, movementInput.moveStrafe)
    }

    fun calcMoveYaw(yaw: Float, moveForward: Float, moveStrafe: Float): Double {
        val moveYaw = if (moveForward == 0.0f && moveStrafe == 0.0f) 0.0 else MathUtilKt.toDegree(atan2(moveForward, moveStrafe).toFloat()).toDouble() - 90.0
        return MathUtilKt.toRadians(RotationUtils.normalizeAngle(yaw.toDouble() + moveYaw))
    }

    fun calcMovementInput(forward: Boolean, backward: Boolean, left: Boolean, right: Boolean, up: Boolean, down: Boolean): Vec3f {
        var moveForward = 0.0f
        var moveStrafing = 0.0f
        var moveVertical = 0.0f
        if (forward) moveForward += 1.0f
        if (backward) moveForward -= 1.0f
        if (left) moveStrafing += 1.0f
        if (right) moveStrafing -= 1.0f
        if (up) moveVertical += 1.0f
        if (down) moveVertical -= 1.0f
        return Vec3f(moveStrafing, moveVertical, moveForward)
    }

    fun EntityLivingBase.applySpeedPotionEffects(speed: Double): Double {
        var result = speed
        getActivePotionEffect(MobEffects.field_76424_c)?.let {
            result += speed * (it.amplifier + 1.0) * 0.2
        }
        getActivePotionEffect(MobEffects.field_76421_d)?.let {
            result -= speed * (it.amplifier + 1.0) * 0.15
        }
        return result
    }

    fun EntityLivingBase.getSpeedEffectMultiplier(): Double {
        var result = 1.0
        getActivePotionEffect(MobEffects.field_76424_c)?.let {
            result += (it.amplifier + 1.0) * 0.2
        }
        getActivePotionEffect(MobEffects.field_76421_d)?.let {
            result -= (it.amplifier + 1.0) * 0.15
        }
        return result
    }

    fun EntityLivingBase.applyJumpBoostPotionEffects(motion: Double): Double {
        return getActivePotionEffect(MobEffects.field_76430_j)?.let {
            motion + (it.amplifier + 1.0) * 0.2
        } ?: motion
    }

    fun EntityPlayerSP.isCentered(center: BlockPos): Boolean {
        return isCentered(center.x + 0.5, center.z + 0.5)
    }

    fun EntityPlayerSP.isCentered(center: Vec3d): Boolean {
        return isCentered(center.x, center.z)
    }

    fun EntityPlayerSP.isCentered(x: Double, z: Double): Boolean {
        return abs(posX - x) < 0.2 && abs(posZ - z) < 0.2
    }

    fun MovementInput.resetMove() {
        moveForward = 0.0f
        moveStrafe = 0.0f
        forwardKeyDown = false
        backKeyDown = false
        leftKeyDown = false
        rightKeyDown = false
    }

    fun MovementInput.resetJumpSneak() {
        jump = false
        sneak = false
    }
}
