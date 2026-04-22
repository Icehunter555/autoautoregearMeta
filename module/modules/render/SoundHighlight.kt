package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import net.minecraft.client.resources.I18n
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import java.util.concurrent.ConcurrentHashMap

object SoundHighlight : Module(
    "SoundHighlight",
    category = Category.RENDER,
    description = "highlight sounds"
) {
    private val textColor by setting(this, ColorSetting(settingName("Text Color"), ColorRGB(255, 255, 255)))
    private val range by setting(this, IntegerSetting(settingName("Range"), 8, 1..16, 1))

    private val soundMap = ConcurrentHashMap<Vec3d, Pair<String, Long>>()

    init {
        onDisable {
            soundMap.clear()
        }

        safeListener<PacketEvent.PostReceive> {
            val packet = it.packet
            if (packet is SPacketSoundEffect) {
                val subtitleKey = "subtitles.${packet.sound.soundName.path}"
                val path = I18n.format(subtitleKey)
                if (!path.contains("subtitles.")) {
                    val vec = Vec3d(packet.x, packet.y, packet.z)
                    if (player.getDistance(vec.x, vec.y, vec.z) <= range) {
                        soundMap[vec] = path to 255L
                    }
                }
            }
        }

        safeListener<Render2DEvent.Absolute> {
            for ((pos, info) in soundMap) {
                val text = info.first
                val alpha = info.second

                val center = pos.add(0.0, 0.25, 0.0)
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                val scale = (9.0f / Math.pow(1.5, distFactor).toFloat()).coerceAtLeast(1.0f)

                val renderColor = textColor.withAlpha(MathHelper.clamp(alpha.toInt(), 4, 255))

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f
                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, renderColor, scale)

                soundMap[pos] = text to alpha - 1L
            }
            soundMap.entries.removeIf { it.value.second <= 10L }
        }
    }
}
