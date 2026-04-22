package dev.wizard.meta.util.interfaces

import dev.wizard.meta.util.extension.rootName

interface Nameable {
    val name: CharSequence

    val nameAsString: String
        get() = name.toString()

    val internalName: String
        get() = nameAsString.replace(" ", "")

    val allNames: Set<CharSequence>
        get() = setOf(name, internalName, rootName)
}
