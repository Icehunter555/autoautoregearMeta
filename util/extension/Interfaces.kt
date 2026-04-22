package dev.wizard.meta.util.extension

import dev.wizard.meta.translation.TranslationKey
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.interfaces.Nameable

val DisplayEnum.rootName: String
    get() {
        val name = displayName
        return (name as? TranslationKey)?.rootString ?: name.toString()
    }

val Nameable.rootName: String
    get() {
        val n = name
        return (n as? TranslationKey)?.rootString ?: n.toString()
    }
