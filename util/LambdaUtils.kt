package dev.wizard.meta.util

import dev.wizard.meta.setting.settings.AbstractSetting

val BOOLEAN_SUPPLIER_FALSE: () -> Boolean = { false }

fun <T : Any> AbstractSetting<T>.notAtValue(value: T): () -> Boolean {
    return { this.value != value }
}

fun <T : Any> AbstractSetting<T>.atValue(value: T): () -> Boolean {
    return { this.value == value }
}

fun <T : Any> AbstractSetting<T>.atValue(value1: T, value2: T): () -> Boolean {
    return { this.value == value1 || this.value == value2 }
}

fun AbstractSetting<Boolean>.atTrue(): () -> Boolean {
    return { this.value }
}

fun AbstractSetting<Boolean>.atFalse(): () -> Boolean {
    return { !this.value }
}

fun (() -> Boolean).atTrue(): () -> Boolean {
    return { this() }
}

fun (() -> Boolean).atFalse(): () -> Boolean {
    return { !this() }
}

fun <T : Any> (() -> T).atValue(value: T): () -> Boolean {
    return { this() == value }
}

fun <T : Any> (() -> T).notAtValue(value: T): () -> Boolean {
    return { this() != value }
}

infix fun (() -> Boolean).or(block: () -> Boolean): () -> Boolean {
    return { this() || block() }
}

infix fun (() -> Boolean).and(block: () -> Boolean): () -> Boolean {
    return { this() && block() }
}

inline fun <T> T.runIf(condition: Boolean, block: (T) -> T): T {
    return if (condition) block(this) else this
}
