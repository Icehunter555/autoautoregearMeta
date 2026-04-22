package dev.wizard.meta.util.text

interface Detector {
    fun detect(input: CharSequence): Boolean

    fun detectNot(input: CharSequence): Boolean {
        return !detect(input)
    }
}
