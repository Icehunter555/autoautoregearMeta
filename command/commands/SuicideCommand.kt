package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import kotlin.concurrent.thread

object SuicideCommand : ClientCommand("suicide", arrayOf("killme", "kill"), "Kills you") {

    private var running = false
    private var worker: Thread? = null

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }

    init {
        execute("/kills u") {
            if (running) return@execute
            running = true
            worker = thread(name = "suicide-command-worker", isDaemon = true) {
                while (running) {
                    try {
                        val safeEvent = SafeClientEvent.instance ?: continue
                        if (safeEvent.mc.world == null || safeEvent.mc.connection == null) {
                            stop()
                        }
                        MessageSendUtils.sendServerMessage(safeEvent, "I no longer wish to be alive")
                        Thread.sleep(1000L)
                        MessageSendUtils.sendServerMessage(safeEvent, "/kill")
                        stop()
                    } catch (e: InterruptedException) {
                        break
                    } catch (t: Throwable) {
                        NoSpamMessage.sendMessage(t::class.java.simpleName)
                        break
                    }
                }
            }
        }

        int("delay") { intArg ->
            execute("/kills u") {
                if (running) return@execute
                running = true
                val delay = getValue(intArg)
                worker = thread(name = "suicide-command-worker", isDaemon = true) {
                    while (running) {
                        try {
                            val safeEvent = SafeClientEvent.instance ?: continue
                            MessageSendUtils.sendServerMessage(safeEvent, "I no longer wish to be alive")
                            Thread.sleep(delay * 1000L)
                            MessageSendUtils.sendServerMessage(safeEvent, "/kill")
                            running = false
                        } catch (e: InterruptedException) {
                            break
                        } catch (t: Throwable) {
                            NoSpamMessage.sendMessage(t::class.java.simpleName)
                            break
                        }
                    }
                }
            }
        }
    }
}
