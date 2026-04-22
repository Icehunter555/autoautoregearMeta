package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.BoxRenderUtils
import dev.wizard.meta.util.accessor.entityID
import dev.wizard.meta.util.accessor.getDamagedBlocks
import dev.wizard.meta.util.math.vector.toVec3dCenter
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

object BreakESP : Module(
    "BreakESP",
    category = Category.RENDER,
    description = "Highlights blocks being broken nearby"
) {
    private val targets by setting(this, EnumSetting(settingName("Targets"), Targets.BOTH))
    private val colorMode by setting(this, EnumSetting(settingName("Color Mode"), ColorMode.STATIC))
    private val staticColor by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 80, 80), { colorMode == ColorMode.STATIC }))
    private val progressColor by setting(this, BooleanSetting(settingName("Progress Color"), true, { colorMode == ColorMode.PROGRESS }))
    private val filledAlpha by setting(this, IntegerSetting(settingName("Filled Alpha"), 42, 0..255, 1))
    private val outlineAlpha by setting(this, IntegerSetting(settingName("Outline Alpha"), 210, 0..255, 1))
    private val lineWidth by setting(this, FloatSetting(settingName("Outline Width"), 2.0f, 0.5f..5.0f, 0.1f))
    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.GROW))
    private val showText by setting(this, BooleanSetting(settingName("Show Text"), true))
    private val showName by setting(this, BooleanSetting(settingName("Show Player Name"), true))
    private val textScale by setting(this, FloatSetting(settingName("Text Scale"), 1.0f, 0.5f..3.0f, 0.1f))

    private val renderer = ESPRenderer()
    private val breakData = LinkedHashMap<Int, BreakInfo>()

    init {
        onDisable {
            breakData.clear()
        }

        listener<Render3DEvent>(1000) {
            val safe = SafeClientEvent.instance ?: return@listener
            breakData.clear()
            val renderGlobal = safe.mc.renderGlobal
            for (progress in renderGlobal.getDamagedBlocks().values) {
                if (isInvalidBreaker(progress)) continue
                breakData[progress.entityID] = BreakInfo(progress.position)
            }
        }

        safeListener<Render3DEvent> {
            renderer.clear()
            renderer.setThickness(lineWidth)

            for ((entityID, info) in breakData) {
                val progress = mc.renderGlobal.getDamagedBlocks().values.firstOrNull {
                    it.entityID == entityID && it.position == info.pos
                } ?: continue

                if (isInvalidBreaker(progress)) continue

                val damage = progress.partialBlockDamage + 1
                val percent = damage / 10.0

                val renderColor = if (colorMode == ColorMode.PROGRESS) {
                    val r = (255 * (1.0 - percent)).toInt().coerceIn(0, 255)
                    val g = (255 * percent).toInt().coerceIn(0, 255)
                    ColorRGB(r, g, 50)
                } else {
                    staticColor
                }

                val box = when (renderMode) {
                    RenderMode.STATIC -> AxisAlignedBB(info.pos)
                    RenderMode.GROW -> BoxRenderUtils.calcGrowBox(info.pos, percent)
                    RenderMode.RISE -> BoxRenderUtils.calcRiseBox(info.pos, percent)
                }

                renderer.setAFilled(filledAlpha)
                renderer.setAOutline(outlineAlpha)
                renderer.add(box, renderColor)
            }
            renderer.render(true)
        }

        safeListener<Render2DEvent.Absolute> {
            if (!showText && !showName) return@safeListener

            for ((entityID, info) in breakData) {
                val progress = mc.renderGlobal.getDamagedBlocks().values.firstOrNull {
                    it.entityID == entityID && it.position == info.pos
                } ?: continue

                if (isInvalidBreaker(progress)) continue

                val damage = progress.partialBlockDamage + 1
                val percent = damage * 10

                val breaker = world.getEntityByID(entityID) as? EntityPlayer
                val name = breaker?.displayNameString ?: breaker?.name ?: ""

                val center = info.pos.toVec3dCenter()
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                val baseScale = (6.0f / Math.pow(2.0, distFactor).toFloat()).coerceAtLeast(1.0f) * textScale

                var yOffset = 0.0f
                if (showText) {
                    val text = "$percent%"
                    val width = MainFontRenderer.getWidth(text, baseScale)
                    MainFontRenderer.drawString(text, screenPos.x.toFloat() - width / 2.0f, screenPos.y.toFloat() + yOffset, ColorRGB(255, 255, 255), baseScale)
                    yOffset += MainFontRenderer.getHeight(baseScale) + 2.0f
                }

                if (showName && name.isNotEmpty()) {
                    val nameScale = baseScale * 0.8f
                    val width = MainFontRenderer.getWidth(name, nameScale)
                    MainFontRenderer.drawString(name, screenPos.x.toFloat() - width / 2.0f, screenPos.y.toFloat() + yOffset, ColorRGB(200, 200, 200), nameScale)
                }
            }
        }
    }

    private fun isInvalidBreaker(progress: DestroyBlockProgress): Boolean {
        val isSelf = progress.entityID == mc.player?.entityId
        return when (targets) {
            Targets.SELF -> !isSelf
            Targets.OTHER -> isSelf
            Targets.BOTH -> false
        }
    }

    private data class BreakInfo(
        val pos: BlockPos,
        var startTime: Long = System.currentTimeMillis(),
        var lastUpdate: Long = startTime
    )

    private enum class ColorMode { STATIC, PROGRESS }
    private enum class RenderMode { STATIC, GROW, RISE }
    private enum class Targets { SELF, OTHER, BOTH }
}
