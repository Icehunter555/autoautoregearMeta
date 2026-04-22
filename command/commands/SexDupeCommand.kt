package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import java.util.*

object SexDupeCommand : ClientCommand("popbobsexdupe", arrayOf("sexdupe", "popbobdupe"), "do the popbob sex dupe") {

    private var worker: Thread? = null
    @Volatile
    private var running = false

    private fun startWorker(delaySeconds: Int, times: Int) {
        if (running) {
            stop()
        }
        running = true
        worker = Thread {
            try {
                for (i in 0 until times) {
                    if (!running) return@Thread
                    val count = Random().nextInt(30) + 1
                    MessageSendUtils.sendServerMessage(this, "I just did the PopBob Sex Dupe and got $count shulkers!")
                    Thread.sleep(delaySeconds * 1000L)
                }
            } catch (e: InterruptedException) {
                // Ignore
            } catch (t: Throwable) {
                NoSpamMessage.sendError("$chatName Error: ${t.message}")
            } finally {
                stop()
            }
        }.apply {
            isDaemon = true
            name = "popbob-thread"
            start()
        }
    }

    fun stop() {
        running = false
        worker?.interrupt()
        worker = null
    }

    init {
        int("delay") { delayArg ->
            int("max") { maxArg ->
                executeSafe {
                    startWorker(getValue(delayArg), getValue(maxArg))
                }
            }
        }

        executeSafe {
            startWorker(5, 5)
        }

        literal("stop") {
            executeSafe {
                stop()
            }
        }
    }
}
