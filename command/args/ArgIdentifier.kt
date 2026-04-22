package dev.wizard.meta.command.args

import dev.wizard.meta.util.interfaces.Nameable

data class ArgIdentifier<T>(override val name: CharSequence) : Nameable
