package dev.wizard.meta.graphics

inline fun <T : GLObject> T.use(block: T.() -> Unit) {
    bind()
    block()
    unbind()
}
