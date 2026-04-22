package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.AddCollisionBoxEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module

object Heaven : Module(
    "Heaven",
    category = Category.RENDER,
    description = "go to heaven on death"
) {
    init {
        safeListener<TickEvent.Post> {
            if (player.isDead) {
                player.motionY = 0.4
            }
        }

        safeListener<AddCollisionBoxEvent> {
            if (it.entity == player && player.isDead) {
                it.collidingBoxes.clear()
            }
        }
    }
}
