package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.combat.AutoRegear
import dev.wizard.meta.module.modules.movement.AutoWalk
import dev.wizard.meta.module.modules.player.AutoPearl
import dev.wizard.meta.util.BaritoneUtils
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.TextFormattingKt
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextFormatting

object RemoteCommand : Module(
    name = "RemoteCommand",
    category = Category.MISC,
    description = "Allow trusted players to send commands"
) {
    private val allow by setting("Allow", Allow.FRIENDS)
    private val detection by setting("Detection", Detection.MESSAGE_ONLY)
    private val custom by setting("Custom", "unchanged") { allow == Allow.CUSTOM || allow == Allow.FRIENDS_AND_CUSTOM }
    private val filterCommands by setting("Filter Commands", true)
    private val noMetaCommands by setting("No Meta Commands", false) { filterCommands }
    private val noBaritoneCommands by setting("No Baritone Commands", false) { filterCommands }
    private val noServerCommands by setting("No Server Comands", true) { filterCommands }
    private val noUnsafeCommands by setting("No Unsafe Commands", true) { filterCommands }
    private val detectionPrefix by setting("Detection Prefix", "@")
    private val showMessage by setting("Show Message", true)

    override fun getHudInfo(): String {
        return allow.displayName.toString()
    }

    init {
        onEnable {
            NoSpamMessage.sendMessage("$chatName Remote control prefix is ${TextFormatting.LIGHT_PURPLE}$detectionPrefix${TextFormatting.RESET}")
        }

        listener<PacketEvent.Receive> { event ->
            if (event.packet !is SPacketChat) return@listener
            val message = event.packet.chatComponent.unformattedText

            when (detection) {
                Detection.MESSAGE_ONLY -> {
                    if (message.contains(" whispers: ")) {
                        val sender = message.substringBefore(" whispers:").trim()
                        val content = message.substringAfter(" whispers: ").trim()
                        sendLog("$chatName [debug] Received whisper from $sender: $content")

                        if (!isValidUser(sender)) {
                            sendLog("$chatName [debug] User $sender is not allowed")
                            return@listener
                        }

                        handleIncoming(content, sender)
                        if (!showMessage) event.cancel()
                    }
                }
                Detection.ALL -> {
                    var sender: String? = null
                    var content: String? = null

                    if (message.contains(" whispers: ")) {
                        sender = message.substringBefore(" whispers:").trim()
                        content = message.substringAfter(" whispers: ").trim()
                    } else if (message.startsWith("<") && message.contains(">")) {
                        sender = message.substringAfter("<").substringBefore(">").trim()
                        content = message.substringAfter("> ").trim()
                    }

                    if (sender != null && content != null) {
                        sendLog("$chatName [debug] Received message from $sender: $content")
                        if (!isValidUser(sender)) {
                            sendLog("$chatName [debug] User $sender is not allowed")
                            return@listener
                        }
                        if (handleIncoming(content, sender) && !showMessage) {
                            event.cancel()
                        }
                    }
                }
            }
        }
    }

    private fun isValidUser(username: String): Boolean {
        return when (allow) {
            Allow.ANYBODY -> true
            Allow.FRIENDS -> FriendManager.isFriend(username)
            Allow.CUSTOM -> isCustomUser(username)
            Allow.FRIENDS_AND_CUSTOM -> FriendManager.isFriend(username) || isCustomUser(username)
        }
    }

    private fun isCustomUser(username: String): Boolean {
        return custom.split(",").any { it.trim().equals(username, true) }
    }

    private fun handleIncoming(message: String, sender: String): Boolean {
        if (!message.startsWith(detectionPrefix)) return false

        if (message.startsWith("${detectionPrefix}cmd")) {
            val command = message.substring(5).trim()
            if (command.isNotEmpty() && checkCommand(command)) {
                sendLog("$chatName [debug] Executing command from $sender: $command")
                
                if (command.startsWith("#") && (!noBaritoneCommands || !filterCommands)) {
                    MessageSendUtils.sendBaritoneCommand(command.substring(1).trim())
                } else if (command.startsWith("/") && (!noServerCommands || !filterCommands)) {
                    MessageSendUtils.sendServerMessage(this, command.substring(1).trim())
                } else if (command.startsWith(";") && (!noMetaCommands || !filterCommands)) {
                    MessageSendUtils.sendTrollCommand(command.substring(1).trim())
                } else {
                    NoSpamMessage.sendWarning("Unrecognized command: $command")
                }
                NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.LIGHT_PURPLE}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.LIGHT_PURPLE}> ${TextFormatting.RESET}${TextFormattingKt.formatValue(command)}")
                return true
            }
        }

        if (message.startsWith("${detectionPrefix}send-message")) {
            val msg = message.substring(14).trim()
            if (msg.isNotEmpty()) {
                sendLog("$chatName [debug] Sending message from $sender: $msg")
                var safeMsg = msg.replaceFirst("/", "_/")
                safeMsg = safeMsg.replaceFirst(CommandManager.prefix, "_${CommandManager.prefix}")
                safeMsg = safeMsg.replaceFirst("#", "_#")
                
                MessageSendUtils.sendServerMessage(this, safeMsg)
                NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.AQUA}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.AQUA}> ${TextFormatting.RESET}${TextFormattingKt.formatValue(safeMsg)}")
                return true
            }
        }

        if (message.startsWith("${detectionPrefix}coords")) {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.BLUE}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.BLUE}> ${TextFormatting.RESET}${TextFormattingKt.formatValue("coords")}")
            MessageSendUtils.sendTrollCommand("sc p $sender")
            return true
        }

        if (message.startsWith("${detectionPrefix}regear")) {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.GOLD}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.GOLD}> ${TextFormatting.RESET}${TextFormattingKt.formatValue("regear")}")
            AutoRegear.placeShulker = true
            return true
        }

        if (message.startsWith("${detectionPrefix}ping")) {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.GRAY}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.GRAY}> ${TextFormatting.RESET}${TextFormattingKt.formatValue("ping")}")
            MessageSendUtils.sendServerMessage(this, "Pong!")
            return true
        }

        if (message.startsWith("${detectionPrefix}stop")) {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.RED}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.RED}> ${TextFormatting.RESET}${TextFormattingKt.formatValue("stop")}")
            BaritoneUtils.cancelEverything()
            AutoWalk.disable()
            AutoPearl.disable()
            return true
        }

        if (message.startsWith("${detectionPrefix}kick")) {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.DARK_RED}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.DARK_RED}> ${TextFormatting.RESET}${TextFormattingKt.formatValue("kick")}")
            MessageSendUtils.sendTrollCommand("kick")
            return true
        }

        if (message.startsWith("${detectionPrefix}help")) {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}[${TextFormatting.RESET}${TextFormatting.GRAY}$sender${TextFormatting.RESET}${TextFormatting.BOLD}] ${TextFormatting.RESET}${TextFormatting.GRAY}> ${TextFormatting.RESET}${TextFormattingKt.formatValue("help")}")
            mc.player.sendChatMessage("/w $sender Commands: ${detectionPrefix}cmd [command], ${detectionPrefix}coords, ${detectionPrefix}kick, ${detectionPrefix}send-message [message], ${detectionPrefix}stop, ${detectionPrefix}ping, ${detectionPrefix}regear, ${detectionPrefix}help")
            return true
        }

        return false
    }

    private fun checkCommand(command: String): Boolean {
        if (!filterCommands) return true
        if (noUnsafeCommands) {
            when (command) {
                ";killme", "/kill", ";crashme", ";kill", ";metaswap", ";suicide", ";loadpoplag" -> return false
                ";lp" -> return false // Original code logic seems to inverse check here? "if (!string.equals(";lp")) return true; return false;" -> returns false only for ;lp
            }
        }
        return true
    }

    private enum class Allow(override val displayName: CharSequence) : DisplayEnum {
        ANYBODY("AnyBody"),
        FRIENDS("Friends"),
        CUSTOM("Custom"),
        FRIENDS_AND_CUSTOM("Friends and Custom")
    }

    private enum class Detection(override val displayName: CharSequence) : DisplayEnum {
        MESSAGE_ONLY("Message Only"), // "MESSAGE_ONLY" wasn't in decompiled enum names, inferred from context or decompiled mapping might be missing proper name. Assuming MESSAGE_ONLY based on logic.
        ALL("All")
    }
}