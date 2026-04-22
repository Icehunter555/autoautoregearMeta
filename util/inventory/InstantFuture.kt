package dev.wizard.meta.util.inventory

object InstantFuture : StepFuture {
    override fun timeout(timeout: Long): Boolean = true
    override fun confirm() {}
}
