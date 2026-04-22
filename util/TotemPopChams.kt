package dev.wizard.meta.util

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.modules.render.PopChams
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.model.ModelPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color

class TotemPopChams(
    var player: EntityOtherPlayerMP?,
    var playerModel: ModelPlayer?,
    var startTime: Long?,
    alphaFill: Double,
    alphaLine: Double,
    val isSelf: Boolean = false,
    val isFriend: Boolean = false
) {
    private val mc = Minecraft.getMinecraft()
    private val lifetime = 8000L

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        val player = player ?: return
        val model = playerModel ?: return
        val start = startTime ?: return
        val elapsed = System.currentTimeMillis() - start

        if (elapsed > lifetime) {
            MinecraftForge.EVENT_BUS.unregister(this)
            return
        }

        when (PopChams.elevatorMode) {
            PopChams.ElevatorMode.UP -> player.posY += 0.05 * event.partialTicks
            PopChams.ElevatorMode.DOWN -> player.posY -= 0.05 * event.partialTicks
            else -> {}
        }

        if (PopChams.spin != 0) {
            player.renderYawOffset += PopChams.spin.toFloat() * event.partialTicks
            player.rotationYaw = player.renderYawOffset
        }

        val colors = if (isSelf) {
            PopChams.selfOutlineColor to PopChams.selfFillColor
        } else if (isFriend) {
            PopChams.friendOutlineColor to PopChams.friendFillColor
        } else {
            PopChams.outlineColor to PopChams.fillColor
        }

        val lineBase = colors.first
        val fillBase = colors.second
        var lineA = lineBase.a
        var fillA = fillBase.a

        if (elapsed > PopChams.fadeStart) {
            val progress = (elapsed - PopChams.fadeStart).toDouble() / (PopChams.fadeTime * 1000.0)
            val factor = 1.0 - progress.coerceAtMost(1.0)
            lineA = (lineA * factor).toInt()
            fillA = (fillA * factor).toInt()
        }

        var scale = 1.0f
        if (PopChams.shrink && elapsed > PopChams.shrinkStart) {
            val progress = (elapsed - PopChams.shrinkStart).toDouble() / (PopChams.shrinkTime * 1000.0)
            scale = (1.0f - progress.coerceAtMost(1.0).toFloat())
        }

        val fillColor = Color(fillBase.r, fillBase.g, fillBase.b, fillA.coerceIn(0, 255))
        val lineColor = Color(lineBase.r, lineBase.g, lineBase.b, lineA.coerceIn(0, 255))

        GL11.glLineWidth(PopChams.lineWidth.toFloat())
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        
        PopChams.prepareGL()
        if (!PopChams.throughWalls) {
            GlStateManager.enableDepth()
        } else {
            GlStateManager.disableDepth()
        }

        glColor(fillColor)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        renderEntity(player, model, scale)

        GlStateManager.enableDepth()
        GlStateManager.depthFunc(515)
        GlStateManager.depthMask(false)
        glColor(lineColor)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
        renderEntity(player, model, scale)

        GlStateManager.depthMask(true)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        PopChams.releaseGL()
    }

    private fun renderEntity(entity: EntityLivingBase, model: ModelPlayer, scale: Float) {
        val renderPos = mc.renderManager
        val x = entity.posX - renderPos.viewerPosX
        val y = entity.posY - renderPos.viewerPosY
        val z = entity.posZ - renderPos.viewerPosZ
        
        var renderY = y
        if (entity.isSneaking) {
            renderY -= 0.125
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), renderY.toFloat(), z.toFloat())
        GlStateManager.rotate(180.0f - entity.renderYawOffset, 0.0f, 1.0f, 0.0f)
        GlStateManager.scale(-scale, -scale, scale)
        GlStateManager.translate(0.0f, -1.5f, 0.0f)
        
        model.setRotationAngles(entity.limbSwing, entity.limbSwingAmount, entity.ticksExisted.toFloat(), entity.rotationYawHead, entity.rotationPitch, 0.0625f, entity)
        model.render(entity, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f)
        GlStateManager.popMatrix()
    }

    private fun glColor(color: Color) {
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
    }
}
