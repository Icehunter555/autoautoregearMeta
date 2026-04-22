package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.esp.DynamicTracerRenderer
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.math.MathUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

object Tracers : Module(
    "Tracers",
    category = Category.RENDER,
    description = "Draws lines to other living entities"
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.ENTITY_TYPE))

    private val players by setting(this, BooleanSetting(settingName("Players"), true, page.atValue(Page.ENTITY_TYPE)))
    private val friends by setting(this, BooleanSetting(settingName("Friends"), false, page.atValue(Page.ENTITY_TYPE) and players.atTrue()))
    private val mobs by setting(this, BooleanSetting(settingName("Mobs"), true, page.atValue(Page.ENTITY_TYPE)))
    private val passive by setting(this, BooleanSetting(settingName("Passive"), false, page.atValue(Page.ENTITY_TYPE) and mobs.atTrue()))
    private val neutral by setting(this, BooleanSetting(settingName("Neutral"), true, page.atValue(Page.ENTITY_TYPE) and mobs.atTrue()))
    private val hostile by setting(this, BooleanSetting(settingName("Hostile"), true, page.atValue(Page.ENTITY_TYPE) and mobs.atTrue()))
    private val range by setting(this, IntegerSetting(settingName("Range"), 64, 8..512, 8, page.atValue(Page.ENTITY_TYPE)))

    private val colorTarget by setting(this, ColorSetting(settingName("Target Color"), ColorRGB(255, 32, 255, 255), true, page.atValue(Page.COLOR)))
    private val colorPlayer by setting(this, ColorSetting(settingName("Player Color"), ColorRGB(255, 160, 240, 255), true, page.atValue(Page.COLOR)))
    private val colorFriend by setting(this, ColorSetting(settingName("Friend Color"), ColorRGB(32, 250, 32, 255), true, page.atValue(Page.COLOR)))
    private val colorPassive by setting(this, ColorSetting(settingName("Passive Mob Color"), ColorRGB(132, 240, 32, 255), true, page.atValue(Page.COLOR)))
    private val colorNeutral by setting(this, ColorSetting(settingName("Neutral Mob Color"), ColorRGB(255, 232, 0, 255), true, page.atValue(Page.COLOR)))
    private val colorHostile by setting(this, ColorSetting(settingName("Hostile Mob Color"), ColorRGB(250, 32, 32, 255), true, page.atValue(Page.COLOR)))
    private val colorFar by setting(this, ColorSetting(settingName("Far Color"), ColorRGB(255, 255, 255, 255), true, page.atValue(Page.COLOR)))

    private val rangedColor by setting(this, BooleanSetting(settingName("Ranged Color"), true, page.atValue(Page.RENDERING)))
    private val colorChangeRange by setting(this, IntegerSetting(settingName("Color Change Range"), 16, 8..128, 8, page.atValue(Page.RENDERING) and rangedColor.atTrue()))
    private val playerOnly by setting(this, BooleanSetting(settingName("Player Only"), true, page.atValue(Page.RENDERING) and rangedColor.atTrue()))
    private val aFar by setting(this, IntegerSetting(settingName("Far Alpha"), 255, 0..255, 1, page.atValue(Page.RENDERING) and rangedColor.atTrue()))
    private val tracerAlpha by setting(this, IntegerSetting(settingName("Tracer Alpha"), 255, 0..255, 1, page.atValue(Page.RENDERING)))
    private val yOffset by setting(this, FloatSetting(settingName("Y Offset"), 0.0f, 0.0f..1.0f, 0.05f, page.atValue(Page.RENDERING)))
    private val lineWidth by setting(this, FloatSetting(settingName("Line Width"), 2.0f, 0.25f..8.0f, 0.25f, page.atValue(Page.RENDERING)))

    private val tracerRenderer = DynamicTracerRenderer()

    init {
        listener<Render3DEvent> {
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
            GlStateManager.glLineWidth(lineWidth)
            GlStateUtils.depth(false)
            tracerRenderer.render(tracerAlpha)
            GlStateUtils.cull(true)
            GlStateUtils.depth(true)
            GlStateManager.glLineWidth(1.0f)
        }

        safeParallelListener<TickEvent.Post> {
            val entityList = EntityUtils.getTargetList(
                arrayOf(players, friends, true),
                arrayOf(mobs, passive, neutral, hostile),
                true, range, false
            )

            tracerRenderer.update {
                for (entity in entityList) {
                    val xOffset = entity.posX - entity.lastTickPosX
                    val yOffset = entity.posY - entity.lastTickPosY
                    val zOffset = entity.posZ - entity.lastTickPosZ
                    val y = entity.posY + (entity.height * this@Tracers.yOffset)

                    putTracer(entity.posX, y, entity.posZ, xOffset, yOffset, zOffset, getColor(this@safeParallelListener, entity))
                }
            }
        }
    }

    private fun getColor(event: SafeClientEvent, entity: Entity): ColorRGB {
        val baseColor = when {
            entity == CombatManager.target -> colorTarget
            FriendManager.isFriend(entity.name) -> colorFriend
            entity is EntityPlayer -> colorPlayer
            else -> when {
                EntityUtils.isPassive(entity) -> colorPassive
                EntityUtils.isNeutral(entity) -> colorNeutral
                else -> colorHostile
            }
        }

        if (!rangedColor || (playerOnly && entity !is EntityPlayer)) return baseColor

        val dist = event.player.getDistance(entity)
        val r = MathUtils.convertRange(dist, 8.0f, colorChangeRange.toFloat(), baseColor.r.toFloat(), colorFar.r.toFloat()).toInt()
        val g = MathUtils.convertRange(dist, 8.0f, colorChangeRange.toFloat(), baseColor.g.toFloat(), colorFar.g.toFloat()).toInt()
        val b = MathUtils.convertRange(dist, 8.0f, colorChangeRange.toFloat(), baseColor.b.toFloat(), colorFar.b.toFloat()).toInt()
        val a = MathUtils.convertRange(dist, 8.0f, colorChangeRange.toFloat(), tracerAlpha.toFloat(), aFar.toFloat()).toInt()

        return ColorRGB(r, g, b, a)
    }

    private enum class Page { ENTITY_TYPE, COLOR, RENDERING }
}
