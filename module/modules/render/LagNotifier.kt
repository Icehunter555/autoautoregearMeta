package dev.wizard.meta.module.modules.render

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.Resolution
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.process.PauseProcess
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.WebUtils
import dev.wizard.meta.util.math.MathUtils
import dev.wizard.meta.util.math.vector.Vec2f
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

object LagNotifier : Module(
    "LagNotifier",
    category = Category.RENDER,
    description = "Displays a warning when the server is lagging"
) {
    private val detectRubberBand by setting(this, BooleanSetting(settingName("Detect Rubber Band"), true))
    private val pauseBaritone by setting(this, BooleanSetting(settingName("Pause Baritone"), true))
    val pauseAutoWalk by setting(this, BooleanSetting(settingName("Pause Auto Walk"), true))
    private val feedback by setting(this, BooleanSetting(settingName("Pause Feedback"), true, { pauseBaritone }))
    private val timeout by setting(this, FloatSetting(settingName("Timeout"), 3.5f, 0.0f..10.0f, 0.5f))

    private val pingTimer = TickTimer(TimeUnit.SECONDS)
    private val lastPacketTimer = TickTimer()
    private val lastRubberBandTimer = TickTimer()

    private var text = ""
    var paused = false
        private set

    init {
        onDisable {
            unpause()
        }

        listener<Render2DEvent.Troll> {
            if (text.isBlank()) return@listener
            val posX = Resolution.trollWidthF / 2.0f - MainFontRenderer.getWidth(text) / 2.0f
            val posY = 80.0f / ClickGUI.getScaleFactor()
            MainFontRenderer.drawString(text, posX, posY, ColorRGB(255, 33, 33))
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        }

        safeListener<TickEvent.Post> {
            if (mc.isSingleplayer) {
                unpause()
                text = ""
            } else {
                val timeoutMillis = (timeout * 1000.0f).toLong()
                if (lastPacketTimer.tick(timeoutMillis)) {
                    if (pingTimer.tickAndReset(1L)) {
                        WebUtils.update()
                    }
                    text = if (WebUtils.isInternetDown()) "Your internet is offline! " else "Server Not Responding! "
                    text += timeDifference(lastPacketTimer.time)
                    pause()
                } else if (detectRubberBand && !lastRubberBandTimer.tick(timeoutMillis)) {
                    text = "RubberBand Detected! ${timeDifference(lastRubberBandTimer.time)}"
                    pause()
                } else {
                    unpause()
                }
            }
        }

        safeListener<PacketEvent.Receive>(2000) {
            lastPacketTimer.reset()
            if (!detectRubberBand || it.packet !is SPacketPlayerPosLook || player.ticksExisted < 20) return@safeListener

            val packet = it.packet as SPacketPlayerPosLook
            val dist = Vec3d(packet.x, packet.y, packet.z).subtract(player.positionVector).lengthVector()
            val rotationDiff = (Vec2f(packet.yaw, packet.pitch) - Vec2f(player)).length()

            if (dist in 0.5..64.0 || rotationDiff > 1.0) {
                lastRubberBandTimer.reset()
            }
        }

        listener<ConnectionEvent.Connect> {
            lastPacketTimer.reset(69420L)
            lastRubberBandTimer.reset(-69420L)
        }
    }

    private fun pause() {
        if (!paused && pauseBaritone && feedback) {
            MessageSendUtils.sendBaritoneMessage("Paused due to lag!")
        }
        PauseProcess.pauseBaritone(this)
        paused = true
    }

    private fun unpause() {
        if (paused && pauseBaritone && feedback) {
            MessageSendUtils.sendBaritoneMessage("Unpaused!")
        }
        PauseProcess.unpauseBaritone(this)
        paused = false
        text = ""
    }

    private fun timeDifference(timeIn: Long): Double {
        return MathUtils.round((System.currentTimeMillis() - timeIn).toDouble() / 1000.0, 1)
    }
}
