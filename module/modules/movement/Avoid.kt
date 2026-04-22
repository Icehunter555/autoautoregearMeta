package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.events.AddCollisionBoxEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB

object Avoid : Module(
    name = "Avoid",
    category = Category.MOVEMENT,
    description = "Prevents walking into bad blocks",
    priority = 1010
) {
    private val webs by setting("Webs", false)
    private val fire by setting("Fire", false)
    private val cactus by setting("Cactus", false)

    init {
        listener<AddCollisionBoxEvent> { event ->
            if (event.entity == player) {
                val block = event.block
                if ((block == Blocks.WEB && webs) || (block == Blocks.FIRE && fire) || (block == Blocks.CACTUS && cactus)) {
                    event.collidingBoxes.add(AxisAlignedBB(event.pos.x.toDouble(), event.pos.y.toDouble(), event.pos.z.toDouble(), event.pos.x + 1.0, event.pos.y + 1.0, event.pos.z + 1.0))
                }
            }
        }
    }
}
