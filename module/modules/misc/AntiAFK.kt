package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.BaritoneUtils
import net.minecraft.util.math.BlockPos
import java.util.Random

object AntiAFK : Module(
    name = "AntiAFK",
    category = Category.MISC,
    description = "Prevents you from being kicked for AFK"
) {
    private val walk = setting("Walk", true)
    private val allowBreak = setting("Allow Breaking Blocks", false) { walk.value }
    private val radius = setting("Radius", 64, 1..200, 1) { walk.value }

    private val random = Random()
    private var startPos: BlockPos? = null

    init {
        walk.listeners.add {
            if (!walk.value) {
                BaritoneUtils.cancel()
            }
        }

        onEnable {
            startPos = mc.player?.position
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (walk.value && !BaritoneUtils.isPathing) {
                val origin = startPos ?: return@safeListener
                val r = radius.value
                val x = origin.x + random.nextInt(r * 2) - r
                val z = origin.z + random.nextInt(r * 2) - r
                BaritoneUtils.walkTo(BlockPos(x, origin.y, z), allowBreak.value)
            }
        }
        
        onDisable {
            if (walk.value) {
                BaritoneUtils.cancel()
            }
        }
    }
}