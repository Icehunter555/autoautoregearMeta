package dev.wizard.meta.gui.hudgui.elements.text

import dev.fastmc.common.collection.CircularArray
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.util.TpsCalculator

object TPS : LabelHud("TPS", category = Category.TEXT, description = "Server TPS") {

    val tpsBuffer = CircularArray(120, 20.0f)

    override fun updateText(event: SafeClientEvent) {
        tpsBuffer.add(TpsCalculator.tickRate)
        addText("%.2f".format(CircularArray.average(tpsBuffer)))
        addText(" tps", secondary = true)
    }
}
