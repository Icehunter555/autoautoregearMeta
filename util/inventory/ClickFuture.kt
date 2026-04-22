package dev.wizard.meta.util.inventory

class ClickFuture(val id: Short) : StepFuture {
    private val time = System.currentTimeMillis()
    private var confirmed = false

    override fun timeout(timeout: Long): Boolean {
        return confirmed || System.currentTimeMillis() - time > timeout
    }

    override fun confirm() {
        confirmed = true
    }
}
