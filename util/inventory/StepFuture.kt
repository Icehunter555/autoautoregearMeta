package dev.wizard.meta.util.inventory

interface StepFuture {
    fun timeout(timeout: Long): Boolean
    fun confirm()
}
