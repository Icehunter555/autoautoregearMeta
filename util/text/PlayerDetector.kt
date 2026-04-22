package dev.wizard.meta.util.text

interface PlayerDetector {
    fun playerName(input: CharSequence): String?
}
