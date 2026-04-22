package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.manager.managers.TimerManager
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.MovementUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.MathUtils
import java.util.*

object PlayerSpeed : LabelHud("Player Speed", category = Category.TEXT, description = "Player movement speed") {

    private val speedUnit by setting(this, "Speed Unit", SpeedUnit.MPS)
    private val averageSpeedTime by setting(this, "Average Speed Ticks", 10, 1..50, 1)
    private val applyTimer by setting(this, "Apply Timer", true)

    private val speedList = ArrayDeque<Double>()

    override fun updateText(event: SafeClientEvent) {
        updateSpeedList(event)
        val averageSpeed = if (speedList.isEmpty()) 0.0 else speedList.sum() / speedList.size
        val displaySpeed = MathUtils.round(averageSpeed * speedUnit.multiplier, 2)
        addText("%.2f".format(displaySpeed))
        addText(speedUnit.displayString, secondary = true)
    }

    private fun updateSpeedList(event: SafeClientEvent) {
        val tps = if (applyTimer) 1000.0 / TimerManager.tickLength.toDouble() else 20.0
        val speed = MovementUtils.getRealSpeed(event.player) * tps
        if (speed > 0.0 || event.player.ticksExisted % 4 == 0) {
            speedList.add(speed)
        } else {
            speedList.pollFirst()
        }
        while (speedList.size > averageSpeedTime) {
            speedList.pollFirst()
        }
    }

    private enum class SpeedUnit(override val displayName: CharSequence, val multiplier: Double) : DisplayEnum {
        MPS("m/s", 1.0),
        KMH("km/h", 3.6);

        val displayString get() = displayName.toString()
    }
}
