package dev.wizard.meta.util.text

interface RegexDetector : Detector, RemovableDetector {
    val regexes: Array<out Regex>

    override fun detect(input: CharSequence): Boolean {
        return regexes.any { it.containsMatchIn(input) }
    }

    fun matchedRegex(input: CharSequence): Regex? {
        return regexes.firstOrNull { it.containsMatchIn(input) }
    }

    override fun removedOrNull(input: CharSequence): CharSequence? {
        val regex = matchedRegex(input) ?: return null
        val result = regex.replace(input, "")
        return if (result.isNotBlank()) result else null
    }
}
