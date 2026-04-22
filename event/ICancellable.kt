package dev.wizard.meta.event

interface ICancellable {
    var cancelled: Boolean

    fun cancel() {
        cancelled = true
    }
}
