package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.entity.player.EntityPlayer
import java.util.*

object PearlNameTags : Module(
    "PearlNametags",
    category = Category.RENDER,
    description = "name tags for pearls"
) {
    private val range by setting(this, FloatSetting(settingName("Range"), 16.0f, 5.0f..200.0f, 10.0f))
    private val highlightSelf by setting(this, BooleanSetting(settingName("Highlight self"), true))
    private val selfColor by setting(this, ColorSetting(settingName("Self Color"), ColorRGB(0, 0, 255), { highlightSelf }))
    private val highlightFriends by setting(this, BooleanSetting(settingName("Friend's Pearls"), true))
    private val friendColor by setting(this, ColorSetting(settingName("Friend Color"), ColorRGB(0, 255, 255), { highlightFriends }))
    private val color by setting(this, ColorSetting(settingName("Color"), ColorRGB(255, 255, 255)))

    private val pearlMap = LinkedHashMap<EntityEnderPearl, EntityPlayer>()

    init {
        onDisable {
            pearlMap.clear()
        }

        safeListener<TickEvent.Post> {
            val loadedPearls = world.loadedEntityList.filterIsInstance<EntityEnderPearl>()
            for (pearl in loadedPearls) {
                if (pearlMap.containsKey(pearl)) continue

                val closestPlayer = world.playerEntities
                    .minByOrNull { it.getDistance(pearl) } ?: continue

                if (closestPlayer == player && !highlightSelf) continue
                if (EntityUtils.isFriend(closestPlayer) && !highlightFriends) continue
                if (!closestPlayer.isEntityAlive) continue
                if (player.getDistance(pearl) > range) continue

                pearlMap[pearl] = closestPlayer
            }

            pearlMap.entries.removeIf { !loadedPearls.contains(it.key) }
        }

        safeListener<Render2DEvent.Absolute> {
            for ((pearl, owner) in pearlMap) {
                if (player.getDistance(pearl) > range) continue

                val text = owner.name
                val pearlCenter = pearl.positionVector.add(0.0, 0.25, 0.0)
                val distFactor = (ProjectionUtils.distToCamera(pearlCenter) - 1.0).coerceAtLeast(0.0)
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(pearlCenter)
                val scale = (9.0f / Math.pow(1.5, distFactor).toFloat()).coerceAtLeast(1.0f)

                val renderColor = when {
                    owner == player -> selfColor
                    EntityUtils.isFriend(owner) -> friendColor
                    else -> color
                }

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f
                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, renderColor, scale)
            }
        }
    }
}
