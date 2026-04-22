package dev.wizard.meta.event

interface Event : EventPosting {
    fun post() {
        post(this)
    }
}
