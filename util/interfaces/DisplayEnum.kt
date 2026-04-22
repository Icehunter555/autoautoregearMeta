package dev.wizard.meta.util.interfaces

interface DisplayEnum {
    val displayName: CharSequence

    val displayString: String
        get() = displayName.toString()
}
