package dev.wizard.meta.event

import dev.wizard.meta.util.interfaces.Nameable

sealed class AbstractListener<F>(
    owner: Any,
    val eventID: Int,
    val priority: Int,
    val function: F
) {
    val ownerName: String = if (owner is Nameable) {
        owner.nameAsString
    } else {
        owner::class.java.simpleName
    }
}
