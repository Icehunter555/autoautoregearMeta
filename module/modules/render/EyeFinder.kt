package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.atTrue
import dev.wizard.meta.util.atValue
import dev.wizard.meta.util.and
import dev.wizard.meta.util.math.vector.distanceTo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d

object EyeFinder : Module(
    "Eye Finder",
    category = Category.RENDER,
    description = "Draw lines from entity's heads to where they are looking",
    alwaysListening = true
) {
    private val page by setting(this, EnumSetting(settingName("Page"), Page.ENTITY_TYPE))
    private val players by setting(this, BooleanSetting(settingName("Players"), true, page.atValue(Page.ENTITY_TYPE)))
    private val friends by setting(this, BooleanSetting(settingName("Friends"), false, page.atValue(Page.RENDERING) and players.atTrue()))
    private val sleeping by setting(this, BooleanSetting(settingName("Sleeping"), false, page.atValue(Page.RENDERING) and players.atTrue()))
    private val mobs by setting(this, BooleanSetting(settingName("Mobs"), true, page.atValue(Page.ENTITY_TYPE)))
    private val passive by setting(this, BooleanSetting(settingName("Passive Mobs"), false, page.atValue(Page.RENDERING) and mobs.atTrue()))
    private val neutral by setting(this, BooleanSetting(settingName("Neutral Mobs"), true, page.atValue(Page.RENDERING) and mobs.atTrue()))
    private val hostile by setting(this, BooleanSetting(settingName("Hostile Mobs"), true, page.atValue(Page.RENDERING) and mobs.atTrue()))
    private val invisible by setting(this, BooleanSetting(settingName("Invisible"), true, page.atValue(Page.ENTITY_TYPE)))
    private val range by setting(this, IntegerSetting(settingName("Range"), 64, 8..128, 8, page.atValue(Page.ENTITY_TYPE)))

    private val r by setting(this, IntegerSetting(settingName("Red"), 255, 0..255, 1, page.atValue(Page.RENDERING)))
    private val g by setting(this, IntegerSetting(settingName("Green"), 255, 0..255, 1, page.atValue(Page.RENDERING)))
    private val b by setting(this, IntegerSetting(settingName("Blue"), 255, 0..255, 1, page.atValue(Page.RENDERING)))
    private val a by setting(this, IntegerSetting(settingName("Alpha"), 200, 0..255, 1, page.atValue(Page.RENDERING)))
    private val thickness by setting(this, FloatSetting(settingName("Thickness"), 2.0f, 0.25f..5.0f, 0.25f, page.atValue(Page.RENDERING)))

    private var resultMap: Map<Entity, Pair<RayTraceResult, Float>> = emptyMap()
    private val renderer = ESPRenderer().apply {
        setAFilled(85)
        setAOutline(255)
        setThrough(true)
    }

    init {
        listener<Render3DEvent> {
            if (resultMap.isEmpty()) return@listener

            renderer.setThickness(thickness)
            for ((entity, pair) in resultMap) {
                drawLine(entity, pair)
            }
            GlStateManager.glLineWidth(thickness)
            RenderUtils3D.draw(1)
            renderer.render(true)
        }

        safeParallelListener<TickEvent.Post> {
            alwaysListening = resultMap.isNotEmpty()

            val entityList = if (isEnabled) {
                EntityUtils.getTargetList(
                    arrayOf(players.value, friends.value, sleeping.value),
                    arrayOf(mobs.value, passive.value, neutral.value, hostile.value),
                    invisible.value, range.value, false
                )
            } else emptyList<EntityLivingBase>()

            val newMap = HashMap<Entity, Pair<RayTraceResult, Float>>()
            for (entry in entityList) {
                val result = getRaytraceResult(this, entry) ?: continue
                newMap[entry] = Pair(result, 0.0f)
            }

            val currentResultMap = resultMap.toMutableMap()
            for ((entity, pair) in currentResultMap) {
                val result = getRaytraceResult(this, entity)
                if (result != null) {
                    val currentAlpha = pair.second
                    val newAlpha = (currentAlpha + 0.07f).coerceAtMost(1.0f)
                    newMap[entity] = Pair(result, newAlpha)
                } else {
                    val newAlpha = pair.second - 0.05f
                    if (newAlpha >= 0.0f) {
                        newMap[entity] = Pair(pair.first, newAlpha)
                    }
                }
            }
            resultMap = newMap
        }
    }

    private fun getRaytraceResult(event: SafeClientEvent, entity: Entity): RayTraceResult? {
        val partialTicks = RenderUtils3D.partialTicks
        val eyePos = entity.getPositionEyes(partialTicks)
        val entityLookVec = entity.getLook(partialTicks).scale(5.0)
        val entityLookEnd = eyePos.add(entityLookVec)

        var rayTraceResult = event.world.rayTraceBlocks(EntityUtils.getEyePosition(entity), entityLookEnd, false, false, true)

        if (rayTraceResult == null || rayTraceResult.typeOfHit == RayTraceResult.Type.MISS) {
            val result = rayTraceResult ?: RayTraceResult(RayTraceResult.Type.MISS, entityLookEnd, null, null)
            for (otherEntity in EntityManager.entity) {
                if (entity.distanceTo(otherEntity) > 10.0 || otherEntity == entity || otherEntity == mc.renderViewEntity) continue
                val box = otherEntity.entityBoundingBox
                if (box.calculateIntercept(eyePos, entityLookEnd) != null) {
                    result.typeOfHit = RayTraceResult.Type.ENTITY
                    result.entityHit = otherEntity
                }
            }
            rayTraceResult = result
        }
        return rayTraceResult
    }

    private fun drawLine(entity: Entity, pair: Pair<RayTraceResult, Float>) {
        val partialTicks = RenderUtils3D.partialTicks
        val eyePos = entity.getPositionEyes(partialTicks)
        val result = pair.first
        val color = ColorRGB(r, g, b, (a * pair.second).toInt())

        RenderUtils3D.putVertex(eyePos.x, eyePos.y, eyePos.z, color)
        RenderUtils3D.putVertex(result.hitVec.x, result.hitVec.y, result.hitVec.z, color)

        if (result.typeOfHit != RayTraceResult.Type.MISS) {
            val box = when (result.typeOfHit) {
                RayTraceResult.Type.BLOCK -> AxisAlignedBB(result.blockPos).grow(0.002)
                RayTraceResult.Type.ENTITY -> {
                    val entityHit = result.entityHit!!
                    val offset = EntityUtils.getInterpolatedAmount(entityHit, partialTicks)
                    entityHit.renderBoundingBox.offset(offset)
                }
                else -> null
            }
            box?.let { renderer.add(it, color) }
        }
    }

    private enum class Page { ENTITY_TYPE, RENDERING }
}
