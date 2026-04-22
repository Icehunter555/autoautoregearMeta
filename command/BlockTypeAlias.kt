package dev.wizard.meta.command

import dev.wizard.meta.command.args.AbstractArg
import dev.wizard.meta.command.args.ArgIdentifier

typealias BuilderBlock<T> = AbstractArg<T>.(ArgIdentifier<T>) -> Unit
typealias ExecuteBlock<E> = suspend E.() -> Unit
