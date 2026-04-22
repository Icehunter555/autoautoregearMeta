package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.interfaces.DisplayEnum
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

object ChinaHat : Module(
    "ChinaHat",
    category = Category.RENDER,
    description = "Renders a china hat over players"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.TARGETS))
    private val self by setting(this, BooleanSetting(settingName("Self"), true, page.atValue(Page.TARGETS)))
    private val allPlayers by setting(this, BooleanSetting(settingName("All Players"), false, page.atValue(Page.TARGETS)))
    private val players by setting(this, BooleanSetting(settingName("Players"), true, page.atValue(Page.TARGETS) and allPlayers.atFalse()))
    private val friends by setting(this, BooleanSetting(settingName("Friends"), false, page.atValue(Page.TARGETS) and allPlayers.atFalse()))

    private val height by setting(this, DoubleSetting(settingName("Height"), 0.3, 0.0..0.7, 0.05, page.atValue(Page.SETTING)))
    private val radius by setting(this, DoubleSetting(settingName("Radius"), 1.0, 0.3..2.0, 0.05, page.atValue(Page.SETTING)))
    private val rotateSpeed by setting(this, DoubleSetting(settingName("Rotate Speed"), 2.0, 0.0..10.0, 0.1, page.atValue(Page.SETTING)))
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255), page.atValue(Page.SETTING)))
    private val alphaTop by setting(this, IntegerSetting(settingName("Alpha Top"), 153, 25..255, 13, page.atValue(Page.SETTING)))
    private val alphaBottom by setting(this, IntegerSetting(settingName("Alpha Bottom"), 77, 25..255, 13, page.atValue(Page.SETTING)))

    private val outline by setting(this, BooleanSetting(settingName("Outline"), false, page.atValue(Page.SETTING)))
    private val outlineWidth by setting(this, FloatSetting(settingName("Outline Width"), 2.0f, 1.0f..5.0f, 0.1f, page.atValue(Page.SETTING) and { outline }))
    private val outlineAlpha by setting(this, IntegerSetting(settingName("Outline Alpha"), 153, 25..255, 13, page.atValue(Page.SETTING) and { outline }))

    init {
        listener<RenderEntityEvent.All.Post> {
            val entity = it.entity
            if (entity !is EntityPlayer) return@listener
            if (!checkEntityType(entity)) return@listener
            if (entity.isElytraFlying) return@listener
            if (entity == mc.renderViewEntity && mc.gameSettings.thirdPersonView == 0) return@listener

            drawHat(entity, it.x, it.y, it.z, it.partialTicks)
        }
    }

    private fun drawHat(entity: EntityPlayer, x: Double, y: Double, z: Double, partialTicks: Float) {
        GlStateManager.pushMatrix()
        val posY = y + entity.height.toDouble() + 0.1 - (if (entity.isSneaking) 0.1 else 0.0)
        GlStateManager.translate(x, posY, z)

        val rotation = (entity.ticksExisted.toFloat() + partialTicks) * -rotateSpeed
        GlStateManager.rotate(rotation.toFloat(), 0.0f, 1.0f, 0.0f)

        mc.renderViewEntity?.let {
            GlStateManager.rotate(-it.rotationYaw, 0.0f, 1.0f, 0.0f)
        }

        val entityRadius = (entity.entityBoundingBox.maxX - entity.entityBoundingBox.minX) * 0.9 * radius

        GlStateUtils.depth(false)
        GlStateUtils.texture2d(false)
        GlStateUtils.blend(true)
        GlStateUtils.lighting(false)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        drawFilledHat(entityRadius)
        if (outline) {
            drawOutline(entityRadius)
        }

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GlStateUtils.texture2d(true)
        GlStateUtils.depth(true)
        GlStateUtils.lighting(true)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
    }

    private fun drawFilledHat(entityRadius: Double) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glColor4f(color.rFloat, color.gFloat, color.bFloat, alphaTop / 255.0f)
        GL11.glVertex3d(0.0, height, 0.0)

        for (i in 0..360 step 5) {
            GL11.glColor4f(color.rFloat, color.gFloat, color.bFloat, alphaBottom / 255.0f)
            val dir = Math.toRadians(i.toDouble() - 180.0)
            val x = -Math.sin(dir) * entityRadius
            val z = Math.cos(dir) * entityRadius
            GL11.glVertex3d(x, 0.0, z)
        }
        GL11.glEnd()
    }

    private fun drawOutline(entityRadius: Double) {
        GlStateManager.glLineWidth(outlineWidth)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        for (i in 0..360 step 5) {
            GL11.glColor4f(color.rFloat, color.gFloat, color.bFloat, outlineAlpha / 255.0f)
            val dir = Math.toRadians(i.toDouble() - 180.0)
            val x = -Math.sin(dir) * entityRadius
            val z = Math.cos(dir) * entityRadius
            GL11.glVertex3d(x, 0.0, z)
        }
        GL11.glEnd()
        GlStateManager.glLineWidth(1.0f)
    }

    private fun checkEntityType(entity: EntityPlayer): Boolean {
        if (entity == mc.player && self) return true
        if (allPlayers) return true
        if (players && !entity.isPlayerSleeping && !entity.isDead && !EntityUtils.isFriend(entity)) return true
        return friends && EntityUtils.isFriend(entity)
    }

    private enum class Page(override val displayName: CharSequence) : DisplayEnum {
        TARGETS("Entities"),
        SETTING("Rendering")
    }
}
