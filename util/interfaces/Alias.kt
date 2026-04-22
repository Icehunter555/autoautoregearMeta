package dev.wizard.meta.util.interfaces

import dev.wizard.meta.util.extension.rootName

interface Alias : Nameable {
    val alias: Array<out CharSequence>

    override val allNames: Set<CharSequence>
        get() = mutableSetOf(name, internalName, rootName).apply {
            addAll(alias)
        }
}
