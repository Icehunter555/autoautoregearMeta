package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.GuiEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.math.CoordinateConverter
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.client.gui.GuiGameOver

object AutoRespawn : Module(
    name = "AutoRespawn",
    category = Category.MISC,
    description = "Automatically respawn after dying"
) {
    private val respawn by setting("Respawn", true)
    private val deathCoords by setting("Save Death Coords", true)
    private val antiGlitchScreen by setting("Anti Glitch Screen", true)
    private val respawnMessage by setting("Respawn Message", false)
    val messageOnRespawn by setting("Message", "/kit 1") { respawnMessage }

    init {
        listener<GuiEvent.Displayed> {
            if (it.screen !is GuiGameOver) return@listener

            if (deathCoords && player.health <= 0.0f) {
                NoSpamMessage.sendMessage("You died at ${CoordinateConverter.asString(player.position)}!", false)
            }

            if (respawnMessage && player.health <= 0.0f) {
                player.sendChatMessage(messageOnRespawn)
            }

            if (respawn || (antiGlitchScreen && player.health > 0.0f)) {
                player.respawnPlayer()
                it.screen = null
            }
        }
    }
}
