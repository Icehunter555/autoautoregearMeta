package dev.wizard.meta.util.text

interface PrefixDetector : Detector, RemovableDetector {
    val prefixes: Array<out CharSequence>

    override fun detect(input: CharSequence): Boolean {
        return prefixes.any { input.startsWith(it) }
    }

    override fun removedOrNull(input: CharSequence): CharSequence? {
        val prefix = prefixes.firstOrNull { input.startsWith(it) } ?: return null
        return input.toString().removePrefix(prefix)
    }
}
