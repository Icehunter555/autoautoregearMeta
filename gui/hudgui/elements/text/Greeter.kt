package dev.wizard.meta.gui.hudgui.elements.text

import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.gui.hudgui.AbstractHudElement
import dev.wizard.meta.gui.hudgui.LabelHud
import dev.wizard.meta.setting.settings.SettingRegister.Companion.setting
import java.time.LocalTime
import kotlin.random.Random

object Greeter : LabelHud("Greeter", category = Category.TEXT, description = "Player Greeter") {

    private val prefix by setting(this, "Prefix", "Welcome, ", visibility = { textMode == TextMode.CUSTOM })
    private val suffix by setting(this, "Suffix", "!", visibility = { textMode == TextMode.CUSTOM })
    private val textMode by setting(this, "Text Mode", TextMode.NORMAL)

    private val greetings = mutableMapOf(
        1 to ("" to " Returns!"),
        2 to ("Welcome back, " to "!"),
        3 to ("Whats up, " to "?"),
        4 to ("Greetings, " to "!"),
        5 to ("Hi, " to "!"),
        6 to ("Welcome, " to "."),
        7 to ("Back again, " to "?"),
        8 to ("Pvp time, " to "?"),
        9 to ("Looking good " to "!"),
        10 to ("Wsg " to "?")
    )

    private var randomGreeting: Int? = null

    override fun updateText(event: SafeClientEvent) {
        if (randomGreeting == null) {
            randomGreeting = greetings.keys.random()
        }
        addText(getCoolPrefix(), addSpace = false to true)
        addText(event.mc.session.username)
        addText(getCoolSuffix(), secondary = true)
    }

    private fun getCoolSuffix(): String {
        return when (textMode) {
            TextMode.NORMAL -> "!"
            TextMode.TIME -> "!"
            TextMode.CUSTOM -> suffix
            TextMode.RANDOM -> greetings[randomGreeting]?.second ?: "err"
        }
    }

    private fun getCoolPrefix(): String {
        return when (textMode) {
            TextMode.NORMAL -> "Welcome to Meta Client,"
            TextMode.TIME -> getTimeBasedGreeting()
            TextMode.CUSTOM -> prefix
            TextMode.RANDOM -> greetings[randomGreeting]?.first ?: "err"
        }
    }

    fun getTimeBasedGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 6..11 -> "Good Morning,"
            in 12..17 -> "Good Afternoon,"
            in 18..23 -> "Good Evening,"
            else -> "Stop staying up late,"
        }
    }

    private enum class TextMode {
        NORMAL, TIME, CUSTOM, RANDOM
    }
}
