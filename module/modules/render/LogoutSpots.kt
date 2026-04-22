package dev.wizard.meta.module.modules.render

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.event.events.render.Render2DEvent
import dev.wizard.meta.event.events.render.Render3DEvent
import dev.wizard.meta.graphics.ESPRenderer
import dev.wizard.meta.graphics.ProjectionUtils
import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.graphics.font.renderer.MainFontRenderer
import dev.wizard.meta.manager.managers.EntityManager
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.extension.synchronized
import dev.wizard.meta.util.math.vector.toVec3d
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import java.util.*

object LogoutSpots : Module(
    "LogoutSpots",
    category = Category.RENDER,
    description = "Logs when a player leaves the game"
) {
    private val range by setting(this, IntegerSetting(settingName("Range"), 200, 10..250, 5))
    private val fillColor by setting(this, ColorSetting(settingName("Fill Color"), ColorRGB(255, 255, 255)))
    private val fillAlpha by setting(this, IntegerSetting(settingName("Fill Alpha"), 35, 0..255, 1))
    private val outlineAlpha by setting(this, IntegerSetting(settingName("Outline Alpha"), 255, 0..255, 1))
    private val textColor by setting(this, ColorSetting(settingName("Text Color"), ColorRGB(255, 255, 255)))
    private val showFriends by setting(this, BooleanSetting(settingName("Show Friends"), false))

    private val removed = LinkedHashSet<EntityPlayer>().synchronized()
    private val loggedPlayers = LinkedHashMap<EntityPlayer, BlockPos>()
    private val timer = TickTimer(TimeUnit.SECONDS)
    private val renderMap = LinkedHashMap<String, BlockPos>()
    private val renderer = ESPRenderer()

    init {
        onDisable {
            loggedPlayers.clear()
            renderMap.clear()
        }

        listener<WorldEvent.Unload> {
            loggedPlayers.clear()
            renderMap.clear()
        }

        listener<WorldEvent.Entity.Remove> {
            if (it.entity is EntityOtherPlayerMP) {
                removed.add(it.entity as EntityPlayer)
            }
        }

        safeListener<Render2DEvent.Absolute> {
            for ((name, pos) in renderMap) {
                if (player.getDistance(pos.x.toDouble(), player.posY, pos.z.toDouble()) > range || (!showFriends && FriendManager.isFriend(name))) continue

                val dist = player.getDistance(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                val text = "$name [%.1f]".format(dist)

                val center = pos.up().toVec3d()
                val screenPos = ProjectionUtils.toAbsoluteScreenPos(center)
                val distFactor = (ProjectionUtils.distToCamera(center) - 1.0).coerceAtLeast(0.0)
                val scale = (6.0f / Math.pow(2.0, distFactor).toFloat()).coerceAtLeast(1.0f)

                val x = MainFontRenderer.getWidth(text, scale) * -0.5f
                val y = MainFontRenderer.getHeight(scale) * -0.5f
                MainFontRenderer.drawString(text, screenPos.x.toFloat() + x, screenPos.y.toFloat() + y, textColor, scale)
            }
        }

        safeListener<Render3DEvent> {
            renderer.setAFilled(fillAlpha)
            renderer.setAOutline(outlineAlpha)
            for ((name, pos) in renderMap) {
                if (player.getDistance(pos.x.toDouble(), player.posY, pos.z.toDouble()) > range || (!showFriends && FriendManager.isFriend(name))) continue

                val box = player.entityBoundingBox.offset(pos.x - player.posX, pos.y - player.posY, pos.z - player.posZ)
                renderer.add(box, fillColor)
            }
            renderer.render(true)
        }

        safeConcurrentListener<TickEvent.Post> {
            for (loadedPlayer in EntityManager.players) {
                if (loadedPlayer !is EntityOtherPlayerMP || EntityUtils.isFakeOrSelf(loadedPlayer)) continue
                if (connection.getPlayerInfo(loadedPlayer.gameProfile.id) == null) continue

                val profile = UUIDManager.getByUUID(loadedPlayer.uniqueID, true)
                if (profile?.name == loadedPlayer.name) {
                    loggedPlayers[loadedPlayer] = EntityUtils.getFlooredPosition(loadedPlayer)
                }
            }

            if (timer.tickAndReset(1L)) {
                val onlineNames = connection.playerInfoMap.map { it.gameProfile.name }.toSet()
                renderMap.entries.removeIf { onlineNames.contains(it.key) }

                loggedPlayers.entries.removeIf { (player, pos) ->
                    if (connection.getPlayerInfo(player.gameProfile.id) == null) {
                        handleLogout(player, pos)
                        true
                    } else false
                }

                synchronized(removed) {
                    loggedPlayers.keys.removeAll(removed)
                }
                removed.clear()
            }
        }
    }

    private fun handleLogout(player: EntityPlayer, pos: BlockPos) {
        renderMap[player.name] = pos
    }
}
