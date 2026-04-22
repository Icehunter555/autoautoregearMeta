package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.color.setGLColor
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.accessor.renderPosX
import dev.wizard.meta.util.accessor.renderPosY
import dev.wizard.meta.util.accessor.renderPosZ
import dev.wizard.meta.util.math.vector.distanceSqTo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.item.EntityEnderCrystal
import org.lwjgl.opengl.GL20

object CrystalChams : Module(
    "CrystalChams",
    category = Category.RENDER,
    description = "Renders chams for End Crystals"
) {
    val model by setting(this, EnumSetting(settingName("Model"), Model.VANILLA))
    val scale by setting(this, FloatSetting(settingName("Scale"), 1.0f, 0.1f..4.0f, 0.1f))
    val glint by setting(this, BooleanSetting(settingName("Glint"), false))
    val glintDepth by setting(this, BooleanSetting(settingName("Glint Depth"), false, { glint }))
    private val glintColor by setting(this, ColorSetting(settingName("Glint Color"), ColorRGB(255, 255, 255, 125), true, { glint }))
    private val modelColor by setting(this, ColorSetting(settingName("Model Color"), ColorRGB(255, 255, 255, 150), true))
    val spinSpeed by setting(this, FloatSetting(settingName("Spin Speed"), 1.0f, 0.0f..4.0f, 0.1f))
    val floatSpeed by setting(this, FloatSetting(settingName("Float Speed"), 1.0f, 0.0f..4.0f, 0.1f))
    val filled by setting(this, BooleanSetting(settingName("Filled"), true))
    val filledDepth by setting(this, BooleanSetting(settingName("Filled Depth"), true, { filled }))
    private val filledColor by setting(this, ColorSetting(settingName("Filled Color"), ColorRGB(133, 255, 200, 63), true, { filled }))
    val outline by setting(this, BooleanSetting(settingName("Outline"), true))
    val outlineDepth by setting(this, BooleanSetting(settingName("Outline Depth"), false, { outline }))
    private val outlineColor by setting(this, ColorSetting(settingName("Outline Color"), ColorRGB(133, 255, 200, 200), true, { outline }))
    val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 0.25f..4.0f, 0.25f, { outline }))
    val range by setting(this, FloatSetting(settingName("Range"), 16.0f, 0.0f..64.0f, 0.5f))

    @JvmStatic
    fun setFilledColor() = filledColor.setGLColor()

    @JvmStatic
    fun setOutlineColor() = outlineColor.setGLColor()

    @JvmStatic
    fun setGlintColor() = glintColor.setGLColor()

    @JvmStatic
    fun setModelColor() = modelColor.setGLColor()

    init {
        safeListener<RenderEntityEvent.All.Pre> {
            if (it.entity is EntityEnderCrystal && EntityUtils.getViewEntity().distanceSqTo(it.entity) <= range * range) {
                it.cancel()
            }
        }

        safeListener<Render3DEvent> {
            val partialTicks = RenderUtils3D.partialTicks
            val rangeSq = range * range
            val renderer = mc.renderManager.getEntityClassRenderObject<EntityEnderCrystal>(EntityEnderCrystal::class.java) ?: return@safeListener

            GlStateUtils.alpha(true)
            GlStateManager.glLineWidth(width)
            GL20.glUseProgram(0)

            for (crystal in EntityManager.entity) {
                if (crystal !is EntityEnderCrystal || EntityUtils.getViewEntity().distanceSqTo(crystal) > rangeSq) continue

                val x = crystal.posX - mc.renderManager.renderPosX
                val y = crystal.posY - mc.renderManager.renderPosY
                val z = crystal.posZ - mc.renderManager.renderPosZ

                renderer.doRender(crystal, x, y, z, 0.0f, partialTicks)
            }

            GlStateUtils.depth(false)
            GlStateUtils.alpha(false)
        }
    }

    enum class Model { VANILLA, XQZ, NONE }
}
