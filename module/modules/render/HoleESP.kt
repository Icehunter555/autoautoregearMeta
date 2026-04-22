package dev.wizard.meta.module.modules.render

import dev.fastmc.common.getSq
import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.manager.managers.HoleManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.combat.HoleInfo
import dev.wizard.meta.util.combat.HoleType
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.threads.BackgroundScope
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

object HoleESP : Module(
    "HoleESP",
    category = Category.RENDER,
    description = "Show safe holes"
) {
    private val obbyHole by setting(this, BooleanSetting(settingName("Obby Hole"), true))
    private val twoBlocksHole by setting(this, BooleanSetting(settingName("2 Blocks Hole"), true))
    private val fourBlocksHole by setting(this, BooleanSetting(settingName("4 Blocks Hole"), true))
    private val trappedHole by setting(this, BooleanSetting(settingName("Trapped Hole"), true))

    var bedrockColor by setting(this, ColorSetting(settingName("Bedrock Color"), ColorRGB(31, 255, 31)))
    var obbyColor by setting(this, ColorSetting(settingName("Obby Color"), ColorRGB(255, 255, 31), { obbyHole }))
    private val twoBlocksColor by setting(this, ColorSetting(settingName("2 Blocks Color"), ColorRGB(255, 127, 31), { twoBlocksHole }))
    private val fourBlocksColor by setting(this, ColorSetting(settingName("4 Blocks Color"), ColorRGB(255, 127, 31), { fourBlocksHole }))
    var trappedColor by setting(this, ColorSetting(settingName("Trapped Color"), ColorRGB(255, 31, 31), { trappedHole }))

    private val renderMode by setting(this, EnumSetting(settingName("Render Mode"), RenderMode.GLOW))
    private val filled by setting(this, BooleanSetting(settingName("Filled"), true))
    private val outline by setting(this, BooleanSetting(settingName("Outline"), true))
    private val aFilled by setting(this, IntegerSetting(settingName("Filled Alpha"), 63, 0..255, 1, { filled }))
    private val aOutline by setting(this, IntegerSetting(settingName("Outline Alpha"), 255, 0..255, 1, { outline }))
    private val glowHeight by setting(this, FloatSetting(settingName("Glow Height"), 1.0f, 0.25f..4.0f, 0.25f, { renderMode == RenderMode.GLOW }))
    private val flatOutline by setting(this, BooleanSetting(settingName("Flat Outline"), true, { renderMode == RenderMode.GLOW }))
    private val width by setting(this, FloatSetting(settingName("Width"), 2.0f, 1.0f..8.0f, 0.1f, { outline }))

    private val range by setting(this, IntegerSetting(settingName("Range"), 16, 4..32, 1))
    private val verticleRange by setting(this, IntegerSetting(settingName("Vertical Range"), 8, 4..16, 1))

    private val renderer = ESPRenderer()
    private val timer = TickTimer()

    override fun getHudInfo(): String = renderer.size.toString()

    init {
        safeListener<Render3DEvent> {
            if (timer.tickAndReset(41L)) {
                updateRenderer()
            }
            if (renderMode == RenderMode.GLOW) {
                renderGlowESP()
            } else {
                renderer.render(false)
            }
        }
    }

    private fun renderGlowESP() {
        GlStateUtils.depth(false)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GlStateManager.glLineWidth(width)

        if (filled) {
            GlStateUtils.cull(false)
            renderGlowESPFilled()
            RenderUtils3D.draw(GL11.GL_QUADS)
            GlStateUtils.cull(true)
        }

        if (outline) {
            renderGlowESPOutline()
            RenderUtils3D.draw(GL11.GL_LINES)
        }

        GlStateUtils.depth(true)
        GlStateManager.glLineWidth(1.0f)
    }

    private fun renderGlowESPFilled() {
        for (info in renderer.toRender) {
            val box = info.box
            val color = info.color

            val fAlpha = aFilled
            val zeroAlpha = 0

            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.withAlpha(fAlpha))

            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight.toDouble(), box.maxZ, color.withAlpha(zeroAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight.toDouble(), box.minZ, color.withAlpha(zeroAlpha))

            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight.toDouble(), box.minZ, color.withAlpha(zeroAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight.toDouble(), box.maxZ, color.withAlpha(zeroAlpha))

            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight.toDouble(), box.minZ, color.withAlpha(zeroAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight.toDouble(), box.minZ, color.withAlpha(zeroAlpha))

            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.withAlpha(fAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight.toDouble(), box.maxZ, color.withAlpha(zeroAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY + glowHeight.toDouble(), box.maxZ, color.withAlpha(zeroAlpha))
        }
    }

    private fun renderGlowESPOutline() {
        for (info in renderer.toRender) {
            val box = info.box
            val color = info.color
            val oAlpha = aOutline

            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.withAlpha(oAlpha))
            RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.withAlpha(oAlpha))

            if (!flatOutline) {
                RenderUtils3D.putVertex(box.minX, box.minY, box.minZ, color.withAlpha(oAlpha))
                RenderUtils3D.putVertex(box.minX, box.minY + glowHeight.toDouble(), box.minZ, color.withAlpha(0))
                RenderUtils3D.putVertex(box.maxX, box.minY, box.minZ, color.withAlpha(oAlpha))
                RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight.toDouble(), box.minZ, color.withAlpha(0))
                RenderUtils3D.putVertex(box.maxX, box.minY, box.maxZ, color.withAlpha(oAlpha))
                RenderUtils3D.putVertex(box.maxX, box.minY + glowHeight.toDouble(), box.maxZ, color.withAlpha(0))
                RenderUtils3D.putVertex(box.minX, box.minY, box.maxZ, color.withAlpha(oAlpha))
                RenderUtils3D.putVertex(box.minX, box.minY + glowHeight.toDouble(), box.maxZ, color.withAlpha(0))
            }
        }
    }

    private fun updateRenderer() {
        val safe = SafeClientEvent.instance ?: return
        BackgroundScope.launch {
            renderer.setAFilled(if (filled) aFilled else 0)
            renderer.setAOutline(if (outline) aOutline else 0)
            renderer.setThickness(width)

            val eyePos = EntityUtils.getEyePosition(safe.player)
            val rangeSq = range * range
            val vRangeSq = verticleRange * verticleRange
            val cached = ArrayList<ESPRenderer.Info>()
            val side = if (renderMode != RenderMode.FLAT) 63 else 1

            for (holeInfo in HoleManager.holeInfos) {
                if (eyePos.distanceSqTo(holeInfo.center) > rangeSq || (eyePos.y - holeInfo.center.y).let { it * it } > vRangeSq) continue

                val color = getColor(holeInfo) ?: continue

                val box = if (renderMode == RenderMode.BLOCK_FLOOR) holeInfo.boundingBox.offset(0.0, -1.0, 0.0) else holeInfo.boundingBox
                cached.add(ESPRenderer.Info(box, color, side))
            }
            renderer.replaceAll(cached)
        }
    }

    private fun getColor(holeInfo: HoleInfo): ColorRGB? {
        if (holeInfo.isTrapped) {
            return if (trappedHole) trappedColor else null
        }
        return when (holeInfo.type) {
            HoleType.BEDROCK -> bedrockColor
            HoleType.OBBY -> if (obbyHole) obbyColor else null
            HoleType.TWO -> if (twoBlocksHole) twoBlocksColor else null
            HoleType.FOUR -> if (fourBlocksHole) fourBlocksColor else null
            else -> null
        }
    }

    private enum class RenderMode { GLOW, FLAT, BLOCK_HOLE, BLOCK_FLOOR }
}
