package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import net.minecraft.entity.Entity

object VanishCommand : ClientCommand("godmode", arrayOf("vanish"), "Allows you to vanish using an entity.") {

    private var vehicle: Entity? = null

    init {
        executeSafe {
            val riding = player.ridingEntity
            if (riding != null && vehicle == null) {
                player.dismountRidingEntity()
                world.removeEntityFromWorld(riding.entityId)
                NoSpamMessage.sendMessage("Vehicle ${riding.name.formatValue()} removed")
                vehicle = riding
            } else {
                vehicle?.let {
                    it.isDead = false
                    world.addEntityToWorld(it.entityId, it)
                    player.startRiding(it, true)
                    NoSpamMessage.sendMessage("Vehicle ${it.name.formatValue()} created")
                    vehicle = null
                } ?: NoSpamMessage.sendMessage("Not riding any vehicles")
            }
        }
    }
}
