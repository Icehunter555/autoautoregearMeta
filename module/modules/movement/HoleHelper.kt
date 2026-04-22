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
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.module.modules.combat.Surround
import dev.wizard.meta.module.modules.exploit.Clip
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.math.RotationUtils
import dev.wizard.meta.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.MovementInputFromOptions
import org.lwjgl.opengl.GL11

object HoleHelper : Module(
    name = "HoleHelper",
    category = Category.MOVEMENT,
    description = "help get u in hole",
    priority = 994,
    modulePriority = 120
) {
    private val range by setting("Range", 1.5f, 0.5f..2.5f, 0.1f)
    private val onMove by setting("On Move", true)
    private val onSneak by setting("On Sneak", true)
    private val timer by setting("Timer", 1.5f, 1.0f..4.0f, 0.01f)
    private val postTimer by setting("Post Timer", 0.8f, 0.01f..1.0f, 0.01f, { timer > 1.0f })
    private val maxPostTicks by setting("Max Post Ticks", 20, 0..50, 1, { timer > 1.0f && postTimer < 1.0f })
    private val disableStrafe by setting("Disable Speed", false)
    private val disableStep by setting("Disable Step", false)

    var hole: HoleInfo? = null
        private set
    private var stuckTicks = 0
    private var ranTicks = 0
    private var isActive = false
    private val renderer = ESPRenderer()

    override fun isActive(): Boolean {
        return isEnabled && isActive
    }

    override fun getHudInfo(): String {
        return "$range, $timer"
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
        val rangeSq = range * range
        return HoleManager.holeInfos.asSequence()
            .filterNot { it.isTrapped }
            .filter { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) <= rangeSq }
            .filter { it.canEnter(world, playerPos) }
            .minByOrNull { distanceSq(player.posX, player.posZ, it.center.x, it.center.z) }
    }

    init {
        onDisable {
            hole = null
            stuckTicks = 0
            ranTicks = 0
            isActive = false
            TimerManager.resetTimer(this)
        }

        listener<Render3DEvent> {
            val it2 = hole ?: return@listener
            val posFrom = EntityUtils.getInterpolatedPos(player, RenderUtils3D.getPartialTicks())
            RenderUtils3D.drawGradientLine(posFrom, it2.center, ColorRGB(255, 255, 255), ClickGUI.primary)
            GlStateManager.glLineWidth(2.0f)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            RenderUtils3D.draw(GL11.GL_LINES)
            GlStateManager.glLineWidth(1.0f)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
        }

        listener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerPosLook) {
                isActive = false
                hole = null
            }
        }

        listener<InputUpdateEvent>(-69) {
            if (it.movementInput is MovementInputFromOptions && isActive) {
                MovementUtils.resetMove(it.movementInput)
            }
        }

        listener<PlayerMoveEvent.Pre>(-10) { event ->
            if (!player.isEntityAlive || EntityUtils.isFlying(player)) {
                isActive = false
                hole = null
                return@listener
            }

            val gameSettings = mc.gameSettings
            val movementInput = gameSettings.keyBindForward.isKeyDown || gameSettings.keyBindBack.isKeyDown || gameSettings.keyBindLeft.isKeyDown || gameSettings.keyBindRight.isKeyDown
            val sneaking = gameSettings.keyBindSneak.isKeyDown

            if (!onMove && movementInput) {
                isActive = false
                hole = null
                return@listener
            }
            if (BaritoneUtils.isActive()) {
                isActive = false
                hole = null
                return@listener
            }
            if (!onSneak && sneaking) {
                isActive = false
                hole = null
                return@listener
            }
            if (Clip.isActive() || Surround.isEnabled) {
                isActive = false
                hole = null
                return@listener
            }

            val currentSpeed = MovementUtils.getSpeed(player)
            if (shouldDisable(currentSpeed)) {
                val ticks = ranTicks
                isActive = false
                hole = null
                if (timer > 0.0f && postTimer < 1.0f && ticks > 0) {
                    val x = (postTimer * ticks - timer * postTimer * ticks) / (timer * (postTimer - 1.0f))
                    val postTicks = Math.min(maxPostTicks, MathUtilKt.ceilToInt(x))
                    TimerManager.modifyTimer(this, 50.0f / postTimer, postTicks)
                }
                return@listener
            }

            findHole()?.let { foundHole ->
                hole = foundHole
                isActive = true
                TimerManager.modifyTimer(this, 50.0f / timer, 5)
                ranTicks++
                if (disableStrafe) Speed.disable()
                if (disableStep) Step.disable()

                val playerPos = player.positionVector
                val yawRad = MathUtilKt.toRadians(Vec2f.getX(RotationUtils.getRotationTo(playerPos, foundHole.center)))
                val dist = Math.hypot(foundHole.center.x - playerPos.x, foundHole.center.z - playerPos.z)
                val baseSpeed = MovementUtils.applySpeedPotionEffects(player as EntityLivingBase, 0.2873)
                val speed = if (player.onGround) baseSpeed else Math.max(currentSpeed + 0.02, baseSpeed)
                val cappedSpeed = Math.min(speed, dist)

                player.motionX = 0.0
                player.motionZ = 0.0
                event.x = -Math.sin(yawRad) * cappedSpeed
                event.z = Math.cos(yawRad) * cappedSpeed
                if (player.collidedHorizontally) stuckTicks++ else stuckTicks = 0
            } ?: run {
                isActive = false
                hole = null
            }
        }
    }
}
