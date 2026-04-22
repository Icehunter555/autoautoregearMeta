package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.realms.RealmsMth
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque

object Breadcrumbs : Module(
    "Breadcrumbs",
    category = Category.RENDER,
    description = "Draws a tail behind as you move",
    alwaysListening = true
) {
    private val clear by setting(this, BooleanSetting(settingName("Clear"), false, isAction = true))
    private val whileDisabled by setting(this, BooleanSetting(settingName("While Disabled"), false))
    private val smoothFactor by setting(this, FloatSetting(settingName("Smooth Factor"), 5.0f, 0.0f..10.0f, 0.25f))
    private val maxDistance by setting(this, IntegerSetting(settingName("Max Distance"), 4096, 1..16384, 1024))
    private val yOffset by setting(this, FloatSetting(settingName("Y Offset"), 0.5f, 0.0f..1.0f, 0.05f))
    private val throughBlocks by setting(this, BooleanSetting(settingName("Through Blocks"), true))
    private val r by setting(this, IntegerSetting(settingName("Red"), 255, 0..255, 1))
    private val g by setting(this, IntegerSetting(settingName("Green"), 166, 0..255, 1))
    private val b by setting(this, IntegerSetting(settingName("Blue"), 188, 0..255, 1))
    private val a by setting(this, IntegerSetting(settingName("Alpha"), 200, 0..255, 1))
    private val thickness by setting(this, FloatSetting(settingName("Line Thickness"), 2.0f, 0.25f..8.0f, 0.25f))

    private val mainList = ConcurrentHashMap<String, HashMap<Int, ArrayDeque<Vec3d>>>()
    private var prevDimension = -2
    private var startTime = -1L
    private var alphaMultiplier = 0.0f
    private var tickCount = 0

    init {
        onToggle {
            if (!whileDisabled) {
                mainList.clear()
            }
        }

        listener<ConnectionEvent.Disconnect> {
            startTime = 0L
            alphaMultiplier = 0.0f
        }

        safeListener<Render3DEvent> {
            if ((mc.integratedServer == null && mc.currentServerData == null) || (isDisabled && !whileDisabled)) {
                return@safeListener
            }

            if (player.dimension != prevDimension) {
                startTime = 0L
                alphaMultiplier = 0.0f
                prevDimension = player.dimension
            }

            if (!shouldRecord(true)) return@safeListener

            val serverIP = getServerIP()
            val dimension = player.dimension
            val renderPosList = addPos(this, serverIP, dimension, RenderUtils3D.partialTicks)
            drawTail(renderPosList)
        }

        safeListener<TickEvent.Post> {
            if (mc.integratedServer == null && mc.currentServerData == null) return@safeListener

            alphaMultiplier = if (isEnabled && shouldRecord(false)) {
                (alphaMultiplier + 0.07f).coerceAtMost(1.0f)
            } else {
                (alphaMultiplier - 0.05f).coerceAtLeast(0.0f)
            }

            if (isDisabled && !whileDisabled) return@safeListener

            if (tickCount < 200) {
                tickCount++
            } else {
                val serverIP = getServerIP()
                val dimensionMap = mainList.getOrPut(serverIP) { HashMap() }
                val posList = dimensionMap.getOrPut(player.dimension) { ArrayDeque() }

                val cutoffPos = posList.lastOrNull { player.distanceTo(it) > maxDistance }
                if (cutoffPos != null) {
                    while (posList.isNotEmpty() && posList.first() != cutoffPos) {
                        posList.removeFirst()
                    }
                }

                dimensionMap[player.dimension] = posList
                tickCount = 0
            }
        }

        clear.valueListeners.add { _, it ->
            if (it) {
                mainList.clear()
                NoSpamMessage.sendMessage("${getChatName()} Cleared!")
                clear.value = false
            }
        }
    }

    private fun drawTail(posList: List<Vec3d>) {
        if (posList.isEmpty() || alphaMultiplier == 0.0f) return

        val offset = Vec3d(0.0, yOffset.toDouble() + 0.05, 0.0)
        val color = ColorRGB(r, g, b, (a * alphaMultiplier).toInt())

        GlStateManager.depthMask(!throughBlocks)
        GlStateManager.glLineWidth(thickness)

        for (pos in posList) {
            val offsetPos = pos.add(offset)
            RenderUtils3D.putVertex(offsetPos.x, offsetPos.y, offsetPos.z, color)
        }
        RenderUtils3D.draw(3)
    }

    private fun addPos(event: SafeClientEvent, serverIP: String, dimension: Int, pTicks: Float): List<Vec3d> {
        var minDist = RealmsMth.sin(-0.05f * smoothFactor * Math.PI.toFloat()) * 2.0f + 2.01f
        if (isDisabled) minDist *= 2.0f

        var currentPos = EntityUtils.getInterpolatedPos(event.player as Entity, pTicks)
        if (event.player.isElytraFlying) {
            currentPos = currentPos.subtract(0.0, 0.5, 0.0)
        }

        val dimensionMap = mainList.getOrPut(serverIP) { HashMap() }
        val posList = dimensionMap.getOrPut(dimension) { ArrayDeque() }

        if (posList.isEmpty() || currentPos.distanceTo(posList.last()) > minDist) {
            posList.addLast(currentPos)
        }

        val returningList = LinkedList(posList)
        returningList.add(currentPos)
        return returningList
    }

    private fun getServerIP(): String {
        return mc.currentServerData?.serverIP
            ?: mc.integratedServer?.worldName
            ?: ""
    }

    private fun shouldRecord(reset: Boolean): Boolean {
        if (startTime == 0L) {
            if (reset) startTime = System.currentTimeMillis()
            return false
        }
        return System.currentTimeMillis() - startTime > 1000L
    }
}
