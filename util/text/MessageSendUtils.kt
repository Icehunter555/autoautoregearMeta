package dev.wizard.meta.util.text

import baritone.api.event.events.ChatEvent
import baritone.api.utils.Helper as BaritoneHelper
import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.manager.managers.MessageManager
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.threads.onMainThread
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting

object MessageSendUtils {
    private val mc = Wrapper.getMinecraft()
    private const val MESSAGE_ID = 431136

    fun sendChatMessage(message: String) {
        sendRawMessage(coloredName(TextFormatting.LIGHT_PURPLE) + message)
    }

    fun sendWarningMessage(message: String) {
        sendRawMessage(coloredName(TextFormatting.GOLD) + message)
    }

    fun sendErrorMessage(message: String) {
        sendRawMessage(coloredName(TextFormatting.DARK_RED) + message)
    }

    fun sendWarning(message: String) {
        sendWarningMessage(message)
    }

    fun sendTrollCommand(command: String) {
        CommandManager.runCommand(command.removePrefix(CommandManager.prefix))
    }

    fun sendBaritoneMessage(message: String) {
        BaritoneHelper.HELPER.logDirect(message)
    }

    fun sendBaritoneCommand(vararg args: String) {
        val chatControl = BaritoneUtils.settings?.chatControl
        val prevValue = chatControl?.value
        chatControl?.value = true
        
        val event = ChatEvent(args.joinToString(" "))
        BaritoneUtils.primary?.gameEventHandler?.onSendChatMessage(event)
        
        if (!event.isCancelled && args.getOrNull(0) != "damn") {
            sendBaritoneMessage("Invalid Command! Please view possible commands at https://github.com/cabaletta/baritone/blob/master/USAGE.md")
        }
        
        chatControl?.value = prevValue
    }

    fun AbstractModule.sendServerMessage(message: String) {
        if (message.isBlank()) return
        MessageManager.addMessageToQueue(message, this, modulePriority)
    }

    fun sendServerMessage(receiver: Any, message: String) {
        if (message.isBlank()) return
        MessageManager.addMessageToQueue(message, receiver, 0)
    }

    fun sendRawMessage(message: String) {
        onMainThread {
            mc.ingameGUI.chatGUI.printChatMessage(TextComponentString(message))
        }
    }

    private fun coloredName(textFormatting: TextFormatting): String {
        return "${TextFormatting.GRAY}[$textFormatting" + "Meta${TextFormatting.GRAY}]${TextFormatting.RESET} "
    }
}
