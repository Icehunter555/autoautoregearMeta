package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import dev.wizard.meta.util.TimeUtils

object Time : LabelHud("Time", category = Category.TEXT, description = "System date and time") {

    private val showDate by setting(this, "Show Date", true)
    private val showTime by setting(this, "Show Time", true)
    private val dateFormat by setting(this, "Date Format", TimeUtils.DateFormat.DDMMYY, visibility = { showDate })
    private val timeFormat by setting(this, "Time Format", TimeUtils.TimeFormat.HHMM, visibility = { showTime })
    private val timeUnit by setting(this, "Time Unit", TimeUtils.TimeUnit.H12, visibility = { showTime })

    override fun updateText(event: SafeClientEvent) {
        if (showDate) {
            addTextLine(TimeUtils.getDate(dateFormat))
        }
        if (showTime) {
            addTextLine(TimeUtils.getTime(timeFormat, timeUnit))
        }
    }
}
