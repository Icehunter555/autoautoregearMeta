package dev.wizard.meta.util.inventory

import dev.wizard.meta.event.SafeClientEvent

interface Step {
    fun run(event: SafeClientEvent): StepFuture
}
