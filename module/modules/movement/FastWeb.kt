package dev.wizard.meta.module.modules.movement

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.accessor.isInWeb
import dev.wizard.meta.util.interfaces.DisplayEnum

object FastWeb : Module(
    name = "FastWeb",
    category = Category.MOVEMENT,
    description = "Fast Web",
    priority = 1010
) {
    private val mode by setting("Mode", Mode.TIMER)
    private val timerSpeed by setting("Timer Speed", 8.0f, 1.0f..10.0f, 0.1f, { mode == Mode.TIMER })
    private val onlySneak by setting("Only Sneak", false)

    private fun sneakCheck(): Boolean {
        return mc.gameSettings.keyBindSneak.isKeyDown || !onlySneak
    }

    init {
        onDisable {
            TimerManager.resetTimer(this)
        }

        onEnable {
            TimerManager.resetTimer(this)
        }

        listener<TickEvent.Post> {
            if (player.isInWeb && sneakCheck()) {
                when (mode) {
                    Mode.TIMER -> {
                        TimerManager.modifyTimer(this, 50.0f / timerSpeed)
                    }
                    Mode.STRICT -> {
                        if (!player.onGround) {
                            TimerManager.modifyTimer(this, 6.25f)
                        }
                    }
                }
            } else {
                TimerManager.resetTimer(this)
            }
        }
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        TIMER("Timer"), STRICT("Strict")
    }
}
