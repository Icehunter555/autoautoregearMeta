package dev.wizard.meta.module.modules.render

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.client.model.ModelBiped
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object Skeleton : Module(
    "Skeleton",
    category = Category.RENDER,
    description = "Draws skeleton lines on players"
) {
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255)))
    private val alpha by setting(this, IntegerSetting(settingName("Alpha"), 255, 0..255, 1))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 1.5f, 0.1f..5.0f, 0.1f))
    private val colorFriends by setting(this, BooleanSetting(settingName("Color Friends"), true))
    private val friendColor by setting(this, ColorSetting(settingName("Friend Color"), ColorRGB(0, 255, 255)))
    private val invisibles by setting(this, BooleanSetting(settingName("Invisibles"), false))
    private val maxDistance by setting(this, DoubleSetting(settingName("Max Distance"), 50.0, 10.0..100.0, 1.0))

    private val rotationList = ConcurrentHashMap<EntityPlayer, Array<FloatArray>>()
    private var registered = false

    init {
        onEnable {
            if (!registered) {
                MinecraftForge.EVENT_BUS.register(this)
                registered = true
            }
        }

        onDisable {
            if (registered) {
                MinecraftForge.EVENT_BUS.unregister(this)
                registered = false
            }
            rotationList.clear()
        }
    }

    @SubscribeEvent
    fun onRenderLivingPost(event: RenderLivingEvent.Post<*>) {
        if (mc.world == null || mc.player == null) return
        val entity = event.entity
        if (entity !is EntityPlayer) return
        if (!shouldRender(entity)) return

        val model = event.renderer.mainModel
        if (model !is ModelBiped) return

        val rotations = getBipedRotations(model)
        val renderColor = getColor(entity)

        prepareGL()
        renderSkeleton(entity, rotations, renderColor, event.x, event.y, event.z)
        releaseGL()
    }

    private fun shouldRender(player: EntityPlayer): Boolean {
        return player != mc.renderViewEntity && player.isEntityAlive && !player.isPlayerSleeping && (invisibles || !player.isInvisible) && mc.player!!.getDistanceSq(player) < maxDistance * maxDistance
    }

    private fun getColor(player: EntityPlayer): Color {
        val isFriend = FriendManager.isFriend(player.name)
        val baseColor = if (colorFriends && isFriend) friendColor else color
        return Color(baseColor.r, baseColor.g, baseColor.b, alpha)
    }

    private fun renderSkeleton(player: EntityPlayer, rotations: Array<FloatArray>, color: Color, renderX: Double, renderY: Double, renderZ: Double) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.pushMatrix()
        glColor(color)
        GlStateManager.translate(renderX, renderY, renderZ)
        GlStateManager.rotate(-player.renderYawOffset, 0.0f, 1.0f, 0.0f)
        GlStateManager.translate(0.0, 0.0, if (player.isSneaking) -0.235 else 0.0)

        val sneak = if (player.isSneaking) 0.6f else 0.75f

        // Legs
        GlStateManager.pushMatrix()
        GlStateManager.translate(-0.125, sneak.toDouble(), 0.0)
        applyRotations(rotations[3])
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(0.0, 0.0, 0.0)
        GL11.glVertex3d(0.0, (-sneak).toDouble(), 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(0.125, sneak.toDouble(), 0.0)
        applyRotations(rotations[4])
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(0.0, 0.0, 0.0)
        GL11.glVertex3d(0.0, (-sneak).toDouble(), 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        GlStateManager.translate(0.0, 0.0, if (player.isSneaking) 0.25 else 0.0)

        GlStateManager.pushMatrix()
        val sneakOffset = if (player.isSneaking) -0.05 else 0.0
        GlStateManager.translate(0.0, sneakOffset, if (player.isSneaking) -0.01725 else 0.0)

        // Arms
        GlStateManager.pushMatrix()
        GlStateManager.translate(-0.375, sneak.toDouble() + 0.55, 0.0)
        applyRotations(rotations[1], true)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(0.0, 0.0, 0.0)
        GL11.glVertex3d(0.0, -0.5, 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(0.375, sneak.toDouble() + 0.55, 0.0)
        applyRotations(rotations[2], true)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(0.0, 0.0, 0.0)
        GL11.glVertex3d(0.0, -0.5, 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        // Head
        GlStateManager.pushMatrix()
        GlStateManager.translate(0.0, sneak.toDouble() + 0.55, 0.0)
        if (rotations[0][0] != 0.0f) {
            GlStateManager.rotate(rotations[0][0] * 57.295776f, 1.0f, 0.0f, 0.0f)
        }
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(0.0, 0.0, 0.0)
        GL11.glVertex3d(0.0, 0.3, 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()
        GlStateManager.popMatrix()

        GlStateManager.rotate(if (player.isSneaking) 25.0f else 0.0f, 1.0f, 0.0f, 0.0f)
        val finalSneakOffset = if (player.isSneaking) -0.16175 else 0.0
        GlStateManager.translate(0.0, finalSneakOffset, if (player.isSneaking) -0.48025 else 0.0)

        // Spine
        GlStateManager.pushMatrix()
        GlStateManager.translate(0.0, sneak.toDouble(), 0.0)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(-0.125, 0.0, 0.0)
        GL11.glVertex3d(0.125, 0.0, 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(0.0, sneak.toDouble(), 0.0)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(0.0, 0.0, 0.0)
        GL11.glVertex3d(0.0, 0.55, 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(0.0, sneak.toDouble() + 0.55, 0.0)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(-0.375, 0.0, 0.0)
        GL11.glVertex3d(0.375, 0.0, 0.0)
        GL11.glEnd()
        GlStateManager.popMatrix()

        GlStateManager.popMatrix()
    }

    private fun applyRotations(rotations: FloatArray, invertZ: Boolean = false) {
        if (rotations[0] != 0.0f) GlStateManager.rotate(rotations[0] * 57.295776f, 1.0f, 0.0f, 0.0f)
        if (rotations[1] != 0.0f) GlStateManager.rotate(rotations[1] * 57.295776f, 0.0f, 1.0f, 0.0f)
        if (rotations[2] != 0.0f) {
            val angle = if (invertZ) -rotations[2] else rotations[2]
            GlStateManager.rotate(angle * 57.295776f, 0.0f, 0.0f, 1.0f)
        }
    }

    private fun glColor(color: Color) {
        GL11.glColor4f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, color.alpha / 255.0f)
    }

    private fun prepareGL() {
        GL11.glBlendFunc(770, 771)
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
        GlStateManager.glLineWidth(lineWidth)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)
        GlStateManager.enableBlend()
        GlStateManager.disableDepth()
        GlStateManager.disableLighting()
        GlStateManager.disableCull()
        GlStateManager.enableAlpha()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
    }

    private fun releaseGL() {
        GlStateManager.enableCull()
        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.enableDepth()
    }

    private fun getBipedRotations(biped: ModelBiped): Array<FloatArray> {
        return arrayOf(
            floatArrayOf(biped.bipedHead.rotateAngleX, biped.bipedHead.rotateAngleY, biped.bipedHead.rotateAngleZ),
            floatArrayOf(biped.bipedRightArm.rotateAngleX, biped.bipedRightArm.rotateAngleY, biped.bipedRightArm.rotateAngleZ),
            floatArrayOf(biped.bipedLeftArm.rotateAngleX, biped.bipedLeftArm.rotateAngleY, biped.bipedLeftArm.rotateAngleZ),
            floatArrayOf(biped.bipedRightLeg.rotateAngleX, biped.bipedRightLeg.rotateAngleY, biped.bipedRightLeg.rotateAngleZ),
            floatArrayOf(biped.bipedLeftLeg.rotateAngleX, biped.bipedLeftLeg.rotateAngleY, biped.bipedLeftLeg.rotateAngleZ)
        )
    }
}
