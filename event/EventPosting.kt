package dev.wizard.meta.event

interface EventPosting {
    val eventBus: EventBus
    fun post(event: Any)
}
