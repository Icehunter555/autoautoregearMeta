package dev.wizard.meta.module.modules.player

import dev.fastmc.common.floorToInt
import dev.fastmc.common.toRadians
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.events.player.PlayerAttackEvent
import dev.wizard.meta.event.events.render.CameraSetupEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.accessor.unpressKey
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.math.vector.Vec3f
import dev.wizard.meta.util.threads.onMainThreadSafe
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.MoverType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.MovementInput
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object Freecam : Module(
    "Freecam",
    category = Category.PLAYER,
    description = "Leave your body and transcend into the realm of the gods"
) {
    private val directionMode by setting(this, EnumSetting(settingName("Flight Mode"), FlightMode.CREATIVE))
    private val horizontalSpeed by setting(this, FloatSetting(settingName("Horizontal Speed"), 20.0f, 1.0f..50.0f, 1.0f))
    private val verticalSpeed by setting(this, FloatSetting(settingName("Vertical Speed"), 20.0f, 1.0f..50.0f, 1.0f, { directionMode == FlightMode.CREATIVE }))
    private val autoRotate by setting(this, BooleanSetting(settingName("Auto Rotate"), true))
    private val arrowKeyMove by setting(this, BooleanSetting(settingName("Arrow Key Move"), true))
    private val relative by setting(this, BooleanSetting(settingName("Relative"), false))

    var cameraGuy: EntityPlayer? = null
        private set
    private const val ENTITY_ID = -6969420

    @JvmStatic
    fun handleTurn(entity: Entity, yaw: Float, pitch: Float, ci: CallbackInfo): Boolean {
        if (INSTANCE.isDisabled()) return false
        val player = mc.player ?: return false
        val camera = cameraGuy ?: return false

        if (entity == player) {
            camera.turn(yaw, pitch)
            ci.cancel()
            return true
        }
        return false
    }

    @JvmStatic
    fun getRenderChunkOffset(playerPos: BlockPos): BlockPos {
        return SafeClientEvent.instance?.let {
            BlockPos((it.player.posX / 16).floorToInt() * 16, (it.player.posY / 16).floorToInt() * 16, (it.player.posZ / 16).floorToInt() * 16)
        } ?: playerPos
    }

    @JvmStatic
    fun getRenderViewEntity(renderViewEntity: EntityPlayer): EntityPlayer {
        return if (INSTANCE.isEnabled && mc.player != null) mc.player!! else renderViewEntity
    }

    init {
        onEnable {
            mc.renderChunksMany = false
        }

        onDisable {
            mc.renderChunksMany = true
            resetCameraGuy()
            mc.player?.let { resetMovementInput(it.movementInput) }
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }

        safeListener<PacketEvent.Send> {
            val packet = it.packet
            if (packet is CPacketUseEntity && packet.getEntityFromWorld(world) == player) {
                it.cancel()
            }
        }

        listener<PlayerAttackEvent> {
            if (it.entity == mc.player) it.cancel()
        }

        safeListener<InputEvent.Keyboard> {
            if (mc.gameSettings.keyBindTogglePerspective.isKeyDown) {
                mc.gameSettings.thirdPersonView = 0
                mc.gameSettings.keyBindTogglePerspective.unpressKey()
            }
        }

        safeListener<TickEvent.Post> {
            if (!player.isEntityAlive) {
                if (cameraGuy != null) resetCameraGuy()
                return@safeListener
            }
            if (cameraGuy == null && player.ticksExisted > 5) {
                spawnCameraGuy(this)
            }
        }

        safeListener<InputUpdateEvent>(9999) {
            if (it.movementInput !is MovementInputFromOptions || BaritoneUtils.isPathing()) return@safeListener

            resetMovementInput(it.movementInput)
            if (BaritoneUtils.isActive()) return@safeListener

            if (autoRotate) updatePlayerRotation(this)
            if (arrowKeyMove) updatePlayerMovement(this)
        }

        safeListener<CameraSetupEvent> {
            if (mc.gameSettings.thirdPersonView <= 0) return@safeListener
            cameraGuy?.let {
                it.yaw = it.rotationYaw
                it.pitch = it.rotationPitch
            }
        }
    }

    private fun spawnCameraGuy(event: SafeClientEvent) {
        val fake = FakeCamera(event.world as WorldClient, event.player)
        event.world.addEntityToWorld(ENTITY_ID, fake)
        mc.setRenderViewEntity(fake)
        resetMovementInput(event.player.movementInput)
        mc.gameSettings.thirdPersonView = 0
        cameraGuy = fake
    }

    private fun resetCameraGuy() {
        cameraGuy = null
        onMainThreadSafe {
            world.removeEntityFromWorld(ENTITY_ID)
            mc.setRenderViewEntity(player)
        }
    }

    private fun resetMovementInput(input: MovementInput?) {
        if (input is MovementInputFromOptions) {
            input.moveForward = 0.0f
            input.moveStrafe = 0.0f
            input.jump = false
            input.sneak = false
        }
    }

    private fun updatePlayerRotation(event: SafeClientEvent) {
        val hit = mc.objectMouseOver ?: return
        if (hit.typeOfHit == RayTraceResult.Type.MISS || hit.hitVec == null) return

        val rotation = RotationUtils.getRotationTo(event, hit.hitVec)
        event.player.rotationYaw = Vec2f.getX(rotation)
        event.player.rotationPitch = Vec2f.getY(rotation)
    }

    private fun updatePlayerMovement(event: SafeClientEvent) {
        val camera = cameraGuy ?: return
        val movementInput = MovementUtils.calcMovementInput(
            Keyboard.isKeyDown(Keyboard.KEY_UP),
            Keyboard.isKeyDown(Keyboard.KEY_DOWN),
            Keyboard.isKeyDown(Keyboard.KEY_LEFT),
            Keyboard.isKeyDown(Keyboard.KEY_RIGHT),
            false, false
        )

        val yawDiff = RotationUtils.normalizeAngle(event.player.rotationYaw - camera.rotationYaw)
        val yawRad = MovementUtils.calcMoveYaw(yawDiff, movementInput.z, movementInput.x)
        val inputTotal = if (movementInput.x == 0.0f && movementInput.z == 0.0f) 0.0f else 1.0f

        event.player.movementInput?.let {
            it.moveForward = Math.cos(yawRad).toFloat() * inputTotal
            it.moveStrafe = -Math.sin(yawRad).toFloat() * inputTotal
            it.forwardKeyDown = it.moveForward > 0.0f
            it.backKeyDown = it.moveForward < 0.0f
            it.leftKeyDown = it.moveStrafe < 0.0f
            it.rightKeyDown = it.moveStrafe > 0.0f
            it.sneak = Keyboard.isKeyDown(157)
        }
    }

    private class FakeCamera(world: WorldClient, val player: EntityPlayerSP) : EntityOtherPlayerMP(world, mc.session.profile) {
        init {
            copyLocationAndAnglesFrom(player)
            capabilities.allowFlying = true
            capabilities.isFlying = true
        }

        override fun onLivingUpdate() {
            inventory.copyInventory(player.inventory)
            updateEntityActionState()

            val movementInput = MovementUtils.calcMovementInput(
                mc.gameSettings.keyBindForward.isKeyDown,
                mc.gameSettings.keyBindBack.isKeyDown,
                mc.gameSettings.keyBindLeft.isKeyDown,
                mc.gameSettings.keyBindRight.isKeyDown,
                mc.gameSettings.keyBindJump.isKeyDown,
                mc.gameSettings.keyBindSneak.isKeyDown
            )

            moveForward = movementInput.z
            moveStrafing = movementInput.x
            moveVertical = movementInput.y
            setSprinting(mc.gameSettings.keyBindSprint.isKeyDown)

            val yawRad = MovementUtils.calcMoveYaw(rotationYaw, moveForward, moveStrafing)
            val speed = horizontalSpeed / 20.0f * Math.min(Math.abs(moveForward) + Math.abs(moveStrafing), 1.0f)

            if (directionMode == FlightMode.THREE_DEE) {
                val pitchRad = rotationPitch.toDouble().toRadians() * moveForward
                motionX = -Math.sin(yawRad) * Math.cos(pitchRad) * speed
                motionY = -Math.sin(pitchRad) * speed
                motionZ = Math.cos(yawRad) * Math.cos(pitchRad) * speed
            } else {
                motionX = -Math.sin(yawRad) * speed
                motionY = moveVertical.toDouble() * (verticalSpeed / 20.0f)
                motionZ = Math.cos(yawRad) * speed
            }

            if (isSprinting) {
                motionX *= 1.5
                motionY *= 1.5
                motionZ *= 1.5
            }

            if (relative) {
                motionX += player.posX - player.prevPosX
                motionY += player.posY - player.prevPosY
                motionZ += player.posZ - player.prevPosZ
            }

            move(MoverType.SELF, motionX, motionY, motionZ)
        }

        override fun getEyeHeight(): Float = 1.65f
        override fun isInvisible(): Boolean = true
        override fun isInvisibleToPlayer(player: EntityPlayer): Boolean = true
        override fun isSpectator(): Boolean = true
    }

    private enum class FlightMode(override val displayName: CharSequence) : DisplayEnum {
        CREATIVE("Creative"), THREE_DEE("3D")
    }
}
