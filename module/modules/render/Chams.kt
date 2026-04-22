package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.color.HueCycler
import dev.wizard.meta.manager.managers.CombatManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.client.CombatSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atFalse
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

object Chams : Module(
    "Chams",
    category = Category.RENDER,
    description = "Modify entity rendering"
) {
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.BOTH))
    private val page by setting(this, EnumSetting(settingName("Page"), Page.ENTITY_TYPE))

    private val self by setting(this, BooleanSetting(settingName("Self"), false, page.atValue(Page.ENTITY_TYPE)))
    private val allEntities by setting(this, BooleanSetting(settingName("All Entities"), false, page.atValue(Page.ENTITY_TYPE)))
    private val items by setting(this, BooleanSetting(settingName("Item"), false, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))
    private val players by setting(this, BooleanSetting(settingName("Player"), true, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))
    private val friends by setting(this, BooleanSetting(settingName("Friend"), false, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))
    private val mobs by setting(this, BooleanSetting(settingName("Mobs"), true, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))
    private val passive by setting(this, BooleanSetting(settingName("Passive"), false, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))
    private val neutral by setting(this, BooleanSetting(settingName("Neutral"), true, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))
    private val hostile by setting(this, BooleanSetting(settingName("Hostile"), true, page.atValue(Page.ENTITY_TYPE) and allEntities.atFalse()))

    // Visible Rendering
    private val shadeMode by setting(this, EnumSetting(settingName("Shade Mode"), ShadeMode.BOTH, page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH)))
    private val texture by setting(this, BooleanSetting(settingName("Texture"), false, page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode.atValue(ShadeMode.FILLED, ShadeMode.BOTH)))
    private val lighting by setting(this, BooleanSetting(settingName("Lighting"), false, page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode.atValue(ShadeMode.FILLED, ShadeMode.BOTH)))
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255), page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH)))
    private val aFilled by setting(this, IntegerSetting(settingName("Filled Alpha"), 127, 0..255, 1, page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode.atValue(ShadeMode.FILLED, ShadeMode.BOTH)))
    private val aOutline by setting(this, IntegerSetting(settingName("Outline Alpha"), 255, 0..255, 1, page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH)))
    private val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 1.0f..8.0f, 0.1f, page.atValue(Page.VISIBLE_RENDERING) and renderMode.atValue(RenderMode.VISIBLE, RenderMode.BOTH) and shadeMode.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH)))

    // Invisible Rendering
    private val shadeModeInvisible by setting(this, EnumSetting(settingName("Shade Mode Invisible"), ShadeMode.OUTLINE, page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH)))
    private val textureInvisible by setting(this, BooleanSetting(settingName("Texture Invisible"), false, page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible.atValue(ShadeMode.FILLED, ShadeMode.BOTH)))
    private val lightingInvisible by setting(this, BooleanSetting(settingName("Lighting Invisible"), false, page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible.atValue(ShadeMode.FILLED, ShadeMode.BOTH)))
    private val colorInvisible by setting(this, ColorSetting(settingName("Color Invisible"), ColorRGB(255, 255, 255), page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH)))
    private val aFilledInvisible by setting(this, IntegerSetting(settingName("Filled Alpha Invisible"), 127, 0..255, 1, page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible.atValue(ShadeMode.FILLED, ShadeMode.BOTH)))
    private val aOutlineInvisible by setting(this, IntegerSetting(settingName("Outline Alpha Invisible"), 255, 0..255, 1, page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH)))
    private val widthInvisible by setting(this, FloatSetting(settingName("Width Invisible"), 2.0f, 1.0f..8.0f, 0.1f, page.atValue(Page.INVISIBLE_RENDERING) and renderMode.atValue(RenderMode.INVISIBLE, RenderMode.BOTH) and shadeModeInvisible.atValue(ShadeMode.OUTLINE, ShadeMode.BOTH)))

    private var cycler = HueCycler(600)

    init {
        listener<RenderEntityEvent.Model.Pre> {
            if (it.cancelled || !checkEntityType(it.entity)) return@listener

            if (renderMode == RenderMode.BOTH) {
                chamsPre(true)
                GlStateManager.depthMask(false)
                renderFilled(it, shadeModeInvisible, textureInvisible, lightingInvisible, colorInvisible, aFilledInvisible)
                renderOutline(it, shadeModeInvisible, textureInvisible, lightingInvisible, colorInvisible, aOutlineInvisible, widthInvisible)
                GlStateManager.depthMask(true)
            }

            if (renderMode == RenderMode.INVISIBLE) {
                chamsPre(true)
                render(it, shadeModeInvisible, textureInvisible, lightingInvisible, colorInvisible, aOutlineInvisible, aFilledInvisible, widthInvisible)
            } else {
                chamsPre(false)
                render(it, shadeMode, texture, lighting, color, aOutline, aFilled, width)
            }
        }

        listener<RenderEntityEvent.Model.Post> {
            if (it.cancelled || !checkEntityType(it.entity)) return@listener
            chamsPost()
        }

        listener<RenderEntityEvent.All.Post> {
            if (!it.cancelled && checkEntityType(it.entity)) {
                GL11.glDepthRange(0.0, 1.0)
            }
        }

        safeListener<TickEvent.Post> {
            cycler = cycler.inc()
        }
    }

    private fun render(event: RenderEntityEvent.Model, shadeMode: ShadeMode, texture: Boolean, lighting: Boolean, color: ColorRGB, aOutline: Int, aFilled: Int, width: Float) {
        when (shadeMode) {
            ShadeMode.OUTLINE -> {
                setColor(color, aOutline)
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
                GlStateManager.glLineWidth(width)
                GlStateUtils.texture2d(false)
                GlStateUtils.lighting(false)
            }
            ShadeMode.FILLED -> {
                setColor(color, aFilled)
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
                GlStateUtils.texture2d(texture)
                GlStateUtils.lighting(lighting)
            }
            ShadeMode.BOTH -> {
                renderFilled(event, shadeMode, texture, lighting, color, aFilled)
                setColor(color, aOutline)
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
                GlStateManager.glLineWidth(width)
                GlStateUtils.texture2d(false)
                GlStateUtils.lighting(false)
            }
        }
    }

    private fun renderOutline(event: RenderEntityEvent.Model, shadeMode: ShadeMode, texture: Boolean, lighting: Boolean, color: ColorRGB, alpha: Int, width: Float) {
        if (shadeMode == ShadeMode.FILLED) return
        setColor(color, alpha)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
        GlStateManager.glLineWidth(width)
        GlStateUtils.texture2d(false)
        GlStateUtils.lighting(false)
        event.render()
        GlStateUtils.texture2d(texture)
        GlStateUtils.lighting(lighting)
    }

    private fun renderFilled(event: RenderEntityEvent.Model, shadeMode: ShadeMode, texture: Boolean, lighting: Boolean, color: ColorRGB, alpha: Int) {
        if (shadeMode == ShadeMode.OUTLINE) return
        setColor(color, alpha)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        GlStateUtils.texture2d(texture)
        GlStateUtils.lighting(lighting)
        event.render()
    }

    private fun setColor(color: ColorRGB, alpha: Int) {
        GL11.glColor4f(color.rFloat, color.gFloat, color.bFloat, alpha / 255.0f)
    }

    private fun chamsPre(invisible: Boolean) {
        if (invisible) {
            GL11.glDepthRange(0.0, 0.01)
        } else {
            GL11.glDepthRange(0.0, 1.0)
        }
        GlStateUtils.blend(true)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
    }

    private fun chamsPost() {
        GlStateUtils.texture2d(true)
        GlStateUtils.lighting(true)
        GlStateUtils.blend(false)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
        GlStateManager.glLineWidth(1.0f)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
    }

    private fun checkEntityType(entity: Entity): Boolean {
        if (CombatSetting.chams.value && entity == CombatManager.target) return true
        if (!self && entity == mc.renderViewEntity) return false

        return if (allEntities) {
            true
        } else if (items && entity is EntityItem) {
            true
        } else if (players && entity is EntityPlayer && EntityUtils.playerTypeCheck(entity, friends, true)) {
            true
        } else {
            EntityUtils.mobTypeSettings(entity, mobs, passive, neutral, hostile)
        }
    }

    private enum class Page { ENTITY_TYPE, VISIBLE_RENDERING, INVISIBLE_RENDERING }
    private enum class RenderMode { VISIBLE, INVISIBLE, BOTH }
    private enum class ShadeMode { OUTLINE, FILLED, BOTH }
}
