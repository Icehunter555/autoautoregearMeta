package dev.wizard.meta.module.modules.client

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.WorldEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.LambdaUtilsKt
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.CPacketUseEntity

object Friends : Module(
    "Friends",
    category = Category.CLIENT,
    description = "Configure the friend management system",
    alwaysListening = true
) {
    private val toggleFriends0 = setting(this, BooleanSetting(settingName("Enable Friends"), true))
    private val noFriendsHit by setting(this, BooleanSetting(settingName("No Friends Hit"), false, LambdaUtilsKt.atTrue(toggleFriends0)))
    private val addedFriendsMessage by setting(this, BooleanSetting(settingName("Added Friends Message"), false))
    private val friendMessage by setting(this, StringSetting(settingName("Message"), "I just added you as a friend on Meta Client!", { addedFriendsMessage }))

    init {
        toggleFriends0.valueListeners.add { _, current ->
            FriendManager.enabled = current
        }

        listener<WorldEvent.Load> {
            toggleFriends0.value = FriendManager.enabled
        }

        listener<WorldEvent.Unload> {
            toggleFriends0.value = FriendManager.enabled
        }

        safeListener<PacketEvent.Send> {
            val packet = it.packet
            if (packet is CPacketUseEntity && packet.action == CPacketUseEntity.Action.ATTACK) {
                if (noFriendsHit) {
                    val entity = packet.getEntityFromWorld(world)
                    if (entity is EntityPlayer && EntityUtils.isFriend(entity)) {
                        it.cancel()
                    }
                }
            }
        }
    }

    fun sendFriendsMessage(profile: PlayerProfile) {
        runSafe {
            if (addedFriendsMessage) {
                val isOnline = connection.playerInfoMap.any { it.gameProfile.name == profile.name }
                if (isOnline) {
                    MessageSendUtils.sendServerMessage("/w ${profile.name} $friendMessage")
                }
            }
        }
    }
}
