package dev.wizard.meta.graphics

interface GLObject {
    val id: Int
    fun bind()
    fun unbind()
    fun destroy()
}

inline fun <T : GLObject> T.use(block: (T) -> Unit) {
    bind()
    try {
        block(this)
    } finally {
        unbind()
    }
}