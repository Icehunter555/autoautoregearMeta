package dev.wizard.meta.event

open class Cancellable : ICancellable {
    override var cancelled: Boolean = false
        set(value) {
            field = field || value
        }
}
