package dev.wizard.meta.util.text

interface RemovableDetector {
    fun removedOrNull(input: CharSequence): CharSequence?
}
