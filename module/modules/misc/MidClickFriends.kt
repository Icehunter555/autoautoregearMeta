package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.backgroundScope
import kotlinx.coroutines.launch
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.RayTraceResult

object MidClickFriends : Module(
    name = "MidClickFriends",
    alias = arrayOf("MCF"),
    category = Category.MISC,
    description = "Middle click players to friend or unfriend them"
) {
    private val timer = TickTimer()
    private var lastPlayer: EntityOtherPlayerMP? = null

    init {
        listener<InputEvent.Mouse> {
            if (it.state || it.button != 2 || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.ENTITY) return@listener

            val entity = mc.objectMouseOver.entityHit
            if (entity !is EntityOtherPlayerMP) return@listener

            val player = entity
            if (timer.tickAndReset(5000L) || (player != lastPlayer && timer.tickAndReset(500L))) {
                if (FriendManager.isFriend(player.name)) {
                    remove(player.name)
                } else {
                    add(player.name)
                }
                lastPlayer = player
            }
        }
    }

    private fun remove(name: String) {
        if (FriendManager.removeFriend(name)) {
            NoSpamMessage.sendMessage("§b$name§r has been unfriended.")
        }
    }

    private fun add(name: String) {
        backgroundScope.launch {
            if (FriendManager.addFriend(name)) {
                NoSpamMessage.sendMessage("§b$name§r has been friended.")
            } else {
                NoSpamMessage.sendMessage("Failed to find UUID of $name")
            }
        }
    }
}