package dev.wizard.meta.command

import dev.wizard.meta.AsyncLoader
import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.ClientExecuteEvent
import dev.wizard.meta.event.IListenerOwner
import dev.wizard.meta.module.modules.client.Settings
import dev.wizard.meta.util.ClassUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.formatValue
import dev.wizard.meta.util.threads.ConcurrentScope
import dev.wizard.meta.util.threads.onMainThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Modifier
import kotlin.system.measureTimeMillis

object CommandManager : AbstractCommandManager<ClientExecuteEvent>(), AsyncLoader<List<Class<out ClientCommand>>> {
    override var deferred: Deferred<List<Class<out ClientCommand>>>? = null

    val prefix: String get() = Settings.prefix

    override suspend fun preLoad0(): List<Class<out ClientCommand>> {
        val classes = AsyncLoader.classes.await()
        var list: List<Class<out ClientCommand>>
        val time = measureTimeMillis {
            list = classes.asSequence()
                .filter { Modifier.isFinal(it.modifiers) }
                .filter { it.name.startsWith("dev.wizard.meta.command.commands") }
                .filter { ClientCommand::class.java.isAssignableFrom(it) }
                .map { it as Class<out ClientCommand> }
                .sortedBy { it.simpleName }
                .toList()
        }
        MetaMod.logger.info("${list.size} commands found, took ${time}ms")
        return list
    }

    override suspend fun load0(input: List<Class<out ClientCommand>>) {
        val time = measureTimeMillis {
            for (clazz in input) {
                register(ClassUtils.getInstance(clazz))
            }
        }
        MetaMod.logger.info("${input.size} commands loaded, took ${time}ms")
    }

    override fun register(builder: CommandBuilder<ClientExecuteEvent>): Command<ClientExecuteEvent> {
        return synchronized(lockObject) {
            (builder as? IListenerOwner)?.subscribe()
            super.register(builder)
        }
    }

    override fun unregister(builder: CommandBuilder<ClientExecuteEvent>): Command<ClientExecuteEvent>? {
        return synchronized(lockObject) {
            (builder as? IListenerOwner)?.unsubscribe()
            super.unregister(builder)
        }
    }

    fun runCommand(string: String) {
        ConcurrentScope.launch {
            val args = tryParseArgument(string) ?: return@launch
            MetaMod.logger.debug("Running command with args: [${args.joinToString()}]")
            try {
                invoke(ClientExecuteEvent(args))
            } catch (e: CommandNotFoundException) {
                handleCommandNotFoundException(args.first())
            } catch (e: SubCommandNotFoundException) {
                handleSubCommandNotFoundException(string, args, e)
            } catch (e: Exception) {
                NoSpamMessage.sendMessage("Error occurred while running command! (${e.message}), check the log for info!")
                MetaMod.logger.warn("Error occurred while running command!", e)
            }
        }
    }

    fun tryParseArgument(string: String): Array<String>? {
        return try {
            parseArguments(string)
        } catch (e: IllegalArgumentException) {
            NoSpamMessage.sendMessage(e.message.toString())
            null
        }
    }

    override suspend fun invoke(event: ClientExecuteEvent) {
        val name = event.args.getOrNull(0) ?: throw IllegalArgumentException("Arguments can not be empty!")
        val command = getCommand(name)
        val finalArg = command.finalArgs.firstOrNull { it.checkArgs(event.args) }
            ?: throw SubCommandNotFoundException(event.args, command)

        onMainThread {
            runBlocking {
                finalArg.invoke(event)
            }
        }
    }

    private fun handleCommandNotFoundException(command: String) {
        NoSpamMessage.sendMessage("Unknown command: ${(prefix + command).formatValue()}. Run ${(prefix + "help").formatValue()} for a list of commands.")
    }

    private suspend fun handleSubCommandNotFoundException(string: String, args: Array<String>, e: SubCommandNotFoundException) {
        val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }
        var message = "Invalid syntax: ${(prefix + string).formatValue()}\n"
        if (bestCommand != null) {
            message += "Did you mean ${(prefix + bestCommand.printArgHelp()).formatValue()}?\n"
        }
        message += "\nRun ${(prefix + "help " + e.command.name).formatValue()} for a list of available arguments."
        NoSpamMessage.sendMessage(message)
    }
}
