package dev.wizard.meta.gui.hudgui.elements.text

import dev.fastmc.common.TickTimer
import dev.wizard.meta.event.events.InputEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.event.listener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.graphics.Easing
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import java.util.ArrayDeque

object CPS : LabelHud("CPS", category = Category.TEXT, description = "Display your clicks per second.") {

    private val averageSpeedTime by setting(this, "Average Speed Time", 2.0f, 1.0f..5.0f, 0.1f, description = "The period of time to measure, in seconds")
    private val timer = TickTimer()
    private val clicks = ArrayDeque<Long>()
    private var currentCps = 0.0f
    private var prevCps = 0.0f

    init {
        listener<InputEvent.Mouse> {
            if (it.state && it.button == 0) {
                clicks.add(System.currentTimeMillis())
            }
        }

        listener<RunGameLoopEvent.Render> {
            if ((currentCps == 0.0f && clicks.isNotEmpty()) || timer.tickAndReset(1000L)) {
                val removeTime = System.currentTimeMillis() - (averageSpeedTime * 1000.0f).toLong()
                while (clicks.isNotEmpty() && clicks.peekFirst() < removeTime) {
                    clicks.removeFirst()
                }
                prevCps = currentCps
                currentCps = clicks.size.toFloat() / averageSpeedTime
            }
        }
    }

    override fun updateText(event: SafeClientEvent) {
        val deltaTime = Easing.toDelta(timer.time, 1000.0f)
        val cps = prevCps + (currentCps - prevCps) * deltaTime
        addText("%.2f".format(cps))
        addText(" CPS", secondary = true)
    }
}
