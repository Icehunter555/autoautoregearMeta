package dev.wizard.meta.module.modules.movement

import dev.fastmc.common.distanceSq
import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.player.InputUpdateEvent
import dev.wizard.meta.event.events.player.PlayerMoveEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.MovementInputFromOptions
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

object HoleSnap : Module(
    name = "HoleSnap",
    category = Category.MOVEMENT,
    description = "Move you into the hole nearby",
    priority = 994,
    modulePriority = 120
) {
    private val downRange by setting("Down Range", 5, 1..8, 1)
    private val upRange by setting("Up Range", 1, 1..8, 1)
    private val hRange by setting("H Range", 4.0f, 1.0f..8.0f, 0.25f)
    private val timer by setting("Timer", 1.5f, 1.0f..4.0f, 0.01f)
    private val postTimer by setting("Post Timer", 0.8f, 0.01f..1.0f, 0.01f, { timer > 1.0f })
    private val maxPostTicks by setting("Max Post Ticks", 20, 0..50, 1, { timer > 1.0f && postTimer < 1.0f })
    private val timeoutTicks by setting("Timeout Ticks", 10, 0..100, 5)
    private val disableStrafe by setting("Disable Speed", false)
    private val disableStep by setting("Disable Step", false)
    private val renderLine by setting("Render Line", true)
    private val lineColor by setting("Line Color", ColorRGB(32, 255, 32), false, ::renderLine)
    private val renderHole by setting("Render Hole", true)
    private val holeFillAlpha by setting("Fill Alpha", 64, 0..255, 1, ::renderHole)
    private val holeOutlineAlpha by setting("Outline Alpha", 255, 0..255, 1, ::renderHole)
    private val holeRenderColor by setting("Color", ColorRGB(32, 255, 32), false, ::renderHole)

    var hole: HoleInfo? = null
        private set

    private var stuckTicks = 0
    private var ranTicks = 0
    private var enabledTicks = 0
    private val renderer = ESPRenderer()
    private var lastStepHeight = 0.6f

    override fun isActive(): Boolean {
        return isEnabled && hole != null
    }

    private fun SafeClientEvent.shouldDisable(currentSpeed: Double): Boolean {
        hole?.let {
            if (player.posY < it.origin.y) return true
        } ?: return false

        if (stuckTicks > 5) {
            if (currentSpeed < 0.05) return true
        }

        if (!player.onGround) return false
        val it = HoleManager.getHoleInfo(player)
        if (!it.isHole) return false
        if (!MovementUtils.isCentered(player, it.center)) return false
        return true
    }

    private fun SafeClientEvent.findHole(): HoleInfo? {
        val playerPos = EntityUtils.getBetterPosition(player)
        val hRangeSq = hRange * hRange
        return HoleManager.holeInfos.asSequence()
            .filterNot { it.isTrapped }
            .filter { checkYRange(playerPos.y, it.origin.y) }
            .filter { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) <= hRangeSq }
            .filter { it.canEnter(world, playerPos) }
            .minByOrNull { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) }
    }

    private fun checkYRange(playerY: Int, holeY: Int): Boolean {
        return if (playerY >= holeY) playerY - holeY <= downRange else holeY - playerY <= -upRange
    }

    init {
        onDisable {
            hole = null
            stuckTicks = 0
            ranTicks = 0
            enabledTicks = 0
            TimerManager.resetTimer(this)
        }

        listener<Render3DEvent> {
            val it2 = hole ?: return@listener
            val posFrom = EntityUtils.getInterpolatedPos(player, RenderUtils3D.getPartialTicks())
            if (renderLine) {
                val color = lineColor.alpha(255)
                RenderUtils3D.putVertex(posFrom.x, posFrom.y, posFrom.z, color)
                RenderUtils3D.putVertex(it2.center.x, it2.center.y, it2.center.z, color)
                GlStateManager.glLineWidth(2.0f)
                GL11.glDisable(GL11.GL_DEPTH_TEST)
                RenderUtils3D.draw(GL11.GL_LINES)
                GlStateManager.glLineWidth(1.0f)
                GL11.glEnable(GL11.GL_DEPTH_TEST)
            }
            if (renderHole) {
                renderer.add(AxisAlignedBB(it2.boundingBox.minX, it2.boundingBox.minY, it2.boundingBox.minZ, it2.boundingBox.maxX, it2.boundingBox.minY + 0.1, it2.boundingBox.maxZ), holeRenderColor)
                renderer.setAOutline(holeOutlineAlpha)
                renderer.setAFilled(holeFillAlpha)
                renderer.render(true)
            }
        }

        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) {
                disable()
            }
        }

        listener<InputUpdateEvent>(-69) {
            if (it.movementInput is MovementInputFromOptions && isActive()) {
                MovementUtils.resetMove(it.movementInput)
            }
        }

        listener<PlayerMoveEvent.Pre>(-10) { event ->
            if (!HolePathFinder.isActive()) {
                if (++enabledTicks > timeoutTicks) {
                    disable()
                    return@listener
                }
            }
            if (!player.isEntityAlive || EntityUtils.isFlying(player)) {
                return@listener
            }
            val currentSpeed = MovementUtils.getSpeed(player)
            if (shouldDisable(currentSpeed)) {
                val ticks = ranTicks
                disable()
                if (timer > 0.0f && postTimer < 1.0f && ticks > 0) {
                    val x = (postTimer * ticks - timer * postTimer * ticks) / (timer * (postTimer - 1.0f))
                    val postTicks = Math.min(maxPostTicks, MathUtilKt.ceilToInt(x))
                    TimerManager.modifyTimer(this, 50.0f / postTimer, postTicks)
                }
                return@listener
            }
            var holeInfo2 = HolePathFinder.hole ?: findHole()
            if (holeInfo2 != null) {
                val it = holeInfo2
                enabledTicks = 0
                if (checkYRange(player.posY.toInt(), it.origin.y) && distanceSq(player.posX, player.posZ, it.center.x, it.center.z) <= MathUtilKt.getSq(hRange)) {
                    TimerManager.modifyTimer(this, 50.0f / timer, 5)
                    ++ranTicks
                    if (disableStrafe) {
                        Speed.disable()
                    }
                    if (disableStep) {
                        Step.disable()
                    }
                    val playerPos = player.positionVector
                    val yawRad = MathUtilKt.toRadians(Vec2f.getX(RotationUtils.getRotationTo(playerPos, it.center)))
                    val dist = Math.hypot(it.center.x - playerPos.x, it.center.z - playerPos.z)
                    val baseSpeed = MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, 0.2873)
                    val speed = if (player.onGround) baseSpeed else Math.max(currentSpeed + 0.02, baseSpeed)
                    val cappedSpeed = Math.min(speed, dist)
                    player.motionX = 0.0
                    player.motionZ = 0.0
                    event.x = -Math.sin(yawRad) * cappedSpeed
                    event.z = Math.cos(yawRad) * cappedSpeed
                    stuckTicks = if (player.collidedHorizontally && HolePathFinder.isDisabled()) stuckTicks + 1 else 0
                    hole = it
                } else {
                    hole = null
                }
            } else {
                hole = null
            }
        }
    }
}
