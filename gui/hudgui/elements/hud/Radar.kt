package dev.wizard.meta.gui.hudgui.elements.hud

import dev.fastmc.common.MathUtilKt
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.HudElement
import dev.wizard.meta.manager.managers.ChunkManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt.and
import dev.wizard.meta.util.LambdaUtilsKt.atTrue
import dev.wizard.meta.util.LambdaUtilsKt.atValue
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11

object Radar : HudElement("Radar", category = Category.HUD, description = "Shows entities and new chunks") {

    private val page by setting(this, "Page", Page.ENTITY)
    private val entity by setting(this, "Entity", true, visibility = atValue(::page, Page.ENTITY))
    private val players by setting(this, "Players", true, visibility = atValue(::page, Page.ENTITY) and atTrue(::entity))
    private val passive by setting(this, "Passive Mobs", false, visibility = atValue(::page, Page.ENTITY) and atTrue(::entity))
    private val neutral by setting(this, "Neutral Mobs", true, visibility = atValue(::page, Page.ENTITY) and atTrue(::entity))
    private val hostile by setting(this, "Hostile Mobs", true, visibility = atValue(::page, Page.ENTITY) and atTrue(::entity))
    private val invisible by setting(this, "Invisible", true, visibility = atValue(::page, Page.ENTITY) and atTrue(::entity))
    private val pointSize by setting(this, "Point Size", 4.0f, 1.0f..16.0f, 0.5f, visibility = atValue(::page, Page.ENTITY) and atTrue(::entity))

    private val chunk by setting(this, "Chunk", false, visibility = atValue(::page, Page.CHUNK))
    private val newChunk by setting(this, "New Chunk", true, visibility = atValue(::page, Page.CHUNK) and atTrue(::chunk))
    private val newChunkColor by setting(this, "New Chunk Color", ColorRGB(255, 31, 31, 63), hasAlpha = true, visibility = atValue(::page, Page.CHUNK) and atTrue(::chunk) and atTrue(::newChunk))
    private val unloadedChunk by setting(this, "Unloaded Chunk", true, visibility = atValue(::page, Page.CHUNK) and atTrue(::chunk))
    private val unloadedChunkColor by setting(this, "Unloaded Chunk Color", ColorRGB(255, 127, 127, 127), hasAlpha = true, visibility = atValue(::page, Page.CHUNK) and atTrue(::chunk) and atTrue(::unloadedChunk))
    private val chunkGrid by setting(this, "Chunk Grid", true, visibility = atValue(::page, Page.CHUNK) and atTrue(::chunk))
    private val gridColor by setting(this, "Grid Color", ColorRGB(127, 127, 127, 63), hasAlpha = true, visibility = atValue(::page, Page.CHUNK) and atTrue(::chunk) and atTrue(::chunkGrid))

    private val radarRange by setting(this, "Radar Range", 64, 8..512, 1)

    override val hudWidth = 100.0f
    override val hudHeight = 100.0f

    private const val halfSize = 50.0
    private const val radius = 48.0f
    private val chunkPos = IntArrayList()
    private val chunkVertices = FloatArrayList()

    override fun renderHud() {
        super.renderHud()
        SafeClientEvent.instance?.let {
            drawBorder(it)
            if (chunk) drawChunk(it)
            if (entity) drawEntity(it)
            drawLabels()
        }
    }

    private fun drawBorder(event: SafeClientEvent) {
        GlStateManager.translate(halfSize, halfSize, 0.0)
        RenderUtils2D.drawCircleFilled(0L, radius, ClickGUI.backGround)
        RenderUtils2D.drawCircleOutline(0L, radius, 1.5f, ClickGUI.text)
        GlStateManager.rotate(-event.player.rotationYaw + 180.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun drawEntity(event: SafeClientEvent) {
        val partialTicks = RenderUtils3D.getPartialTicks()
        val playerPos = EntityUtils.getInterpolatedPos(event.player, partialTicks)
        val posMultiplier = radius / radarRange.toFloat()

        prepareGLPoint()
        RenderUtils2D.putVertex(0.0f, 0.0f, ClickGUI.text)
        for (entity in getEntityList()) {
            val diff = EntityUtils.getInterpolatedPos(entity, partialTicks).subtract(playerPos)
            if (Math.abs(diff.y) > 24.0) continue
            val color = getColor(entity)
            RenderUtils2D.putVertex((diff.x * posMultiplier).toFloat(), (diff.z * posMultiplier).toFloat(), color)
        }
        RenderUtils2D.draw(GL11.GL_POINTS)
        releaseGLPoint()
    }

    private fun getEntityList(): List<EntityLivingBase> {
        return EntityUtils.getTargetList(
            arrayOf(players, true, true),
            arrayOf(true, passive, neutral, hostile),
            invisible,
            radarRange.toDouble(),
            true
        )
    }

    private fun prepareGLPoint() {
        RenderUtils2D.prepareGL()
        GL11.glPointSize(pointSize)
        GL11.glEnable(GL11.GL_POINT_SMOOTH)
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST)
    }

    private fun releaseGLPoint() {
        RenderUtils2D.releaseGL()
        GL11.glPointSize(1.0f)
        GL11.glDisable(GL11.GL_POINT_SMOOTH)
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_FASTEST)
    }

    private fun drawChunk(event: SafeClientEvent) {
        RenderUtils2D.prepareGL()
        val interpolatedPos = EntityUtils.getInterpolatedPos(event.player, RenderUtils3D.getPartialTicks())
        val playerChunkX = MathUtilKt.floorToInt(interpolatedPos.x / 16.0)
        val playerChunkZ = MathUtilKt.floorToInt(interpolatedPos.z / 16.0)
        val posMultiplier = radius / radarRange.toFloat()
        val diffX = (playerChunkX * 16 - interpolatedPos.x) * posMultiplier
        val diffZ = (playerChunkZ * 16 - interpolatedPos.z) * posMultiplier

        drawChunkGrid(diffX, diffZ)
        drawChunkFilled(event, playerChunkX, playerChunkZ)
        RenderUtils2D.releaseGL()
    }

    private fun drawChunkGrid(diffX: Double, diffZ: Double) {
        val chunkDist = radarRange / 16
        val posMultiplier = radius / radarRange.toFloat()
        val chunkPosMultiplier = posMultiplier * 16.0
        val rangeSq = (radarRange * posMultiplier) * (radarRange * posMultiplier)

        chunkPos.clear()
        chunkVertices.clear()
        val drawGrid = chunkGrid

        for (chunkX in -chunkDist..chunkDist) {
            for (chunkZ in -chunkDist..chunkDist) {
                val x1 = (chunkX * chunkPosMultiplier + diffX).toFloat()
                val y1 = (chunkZ * chunkPosMultiplier + diffZ).toFloat()
                val x2 = ((chunkX + 1) * chunkPosMultiplier + diffX).toFloat()
                val y2 = ((chunkZ + 1) * chunkPosMultiplier + diffZ).toFloat()

                if (calcMaxDistSq(x1, y1, x2, y2) < rangeSq) {
                    if (drawGrid) {
                        val color = gridColor
                        RenderUtils2D.putVertex(x1, y1, color)
                        RenderUtils2D.putVertex(x1, y2, color)
                        RenderUtils2D.putVertex(x1, y2, color)
                        RenderUtils2D.putVertex(x2, y2, color)
                        RenderUtils2D.putVertex(x2, y2, color)
                        RenderUtils2D.putVertex(x2, y1, color)
                        RenderUtils2D.putVertex(x2, y1, color)
                        RenderUtils2D.putVertex(x1, y1, color)
                    }
                    chunkPos.add(chunkX)
                    chunkPos.add(chunkZ)
                    chunkVertices.add(x1)
                    chunkVertices.add(y1)
                    chunkVertices.add(x2)
                    chunkVertices.add(y2)
                }
            }
        }
        if (drawGrid) RenderUtils2D.draw(GL11.GL_LINES)
    }

    private fun calcMaxDistSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val maxX = if (x1 + x2 >= 0.0f) x2 else x1
        val maxY = if (y1 + y2 >= 0.0f) y2 else y1
        return maxX * maxX + maxY * maxY
    }

    private fun drawChunkFilled(event: SafeClientEvent, playerChunkX: Int, playerChunkZ: Int) {
        if (unloadedChunk || newChunk) {
            for (i in 0 until chunkPos.size / 2) {
                val chunkX = chunkPos.getInt(i * 2) + playerChunkX
                val chunkZ = chunkPos.getInt(i * 2 + 1) + playerChunkZ
                val chunk = event.world.getChunk(chunkX, chunkZ)
                val x1 = chunkVertices.getFloat(i * 4)
                val y1 = chunkVertices.getFloat(i * 4 + 1)
                val x2 = chunkVertices.getFloat(i * 4 + 2)
                val y2 = chunkVertices.getFloat(i * 4 + 3)

                if (unloadedChunk && (!chunk.isLoaded || !chunk.isTerrainPopulated)) {
                    drawChunkQuad(x1, y1, x2, y2, unloadedChunkColor)
                }
                if (newChunk && ChunkManager.isNewChunk(chunk)) {
                    drawChunkQuad(x1, y1, x2, y2, newChunkColor)
                }
            }
            RenderUtils2D.draw(GL11.GL_QUADS)
        }
    }

    private fun drawChunkQuad(x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        RenderUtils2D.putVertex(x1, y1, color)
        RenderUtils2D.putVertex(x1, y2, color)
        RenderUtils2D.putVertex(x2, y2, color)
        RenderUtils2D.putVertex(x2, y1, color)
    }

    private fun drawLabels() {
        drawLabel("-Z")
        drawLabel("+X")
        drawLabel("+Z")
        drawLabel("-X")
    }

    private fun drawLabel(name: String) {
        MainFontRenderer.drawString(name, MainFontRenderer.getWidth(name, 0.8f) * -0.5f, -radius, ClickGUI.primary, 0.8f, false)
        GlStateManager.rotate(90.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun getColor(entity: EntityLivingBase): Int {
        return if (EntityUtils.isPassive(entity) || FriendManager.isFriend(entity.name)) {
            ColorRGB(32, 224, 32, 224).unbox()
        } else if (EntityUtils.isNeutral(entity)) {
            ColorRGB(255, 240, 32).unbox()
        } else {
            ColorRGB(255, 32, 32).unbox()
        }
    }

    private enum class Page {
        ENTITY, CHUNK
    }
}
