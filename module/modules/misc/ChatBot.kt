package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.command.commands.InsultCommand
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.gui.hudgui.elements.text.TPS
import dev.wizard.meta.manager.managers.UUIDManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.PlayerProfile
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.math.vector.distanceTo
import dev.wizard.meta.util.text.ChatTextUtils
import dev.wizard.meta.util.text.MessageSendUtils
import net.minecraft.network.play.server.SPacketChat
import kotlin.random.Random

object ChatBot : Module(
    name = "ChatBot",
    category = Category.MISC,
    description = "makes you a bot"
) {
    private val page by setting("Page", Page.GENERAL)
    private val respondDelay by setting("Response Delay", 2, 0..10, 1) { page == Page.GENERAL }
    private val detectionMode by setting("Detection", Detection.CHATONLY) { page == Page.GENERAL }
    private val blackList by setting("BlackList", "exploiter1, exploiter2") { page == Page.GENERAL }
    private val invertBlacklist by setting("Invert Blacklist", false) { page == Page.GENERAL }
    private val detectionPrefixes by setting("Detection Prefixes", "!, $, %") { page == Page.GENERAL }
    private val botName by setting("Bot Name", "MetaBot") { page == Page.GENERAL }
    private val greenText by setting("GreenText", false) { page == Page.GENERAL }

    private val pingCommand by setting("Ping Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val tpsCommand by setting("Tps Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val onlineCommand by setting("Online Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val helpCommand by setting("Help Command", HelpCommandMode.SENDCHAT) { page == Page.COMMANDS }
    private val insultCommand by setting("Insult Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val distanceCommand by setting("Distance Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val gayCommand by setting("Gay Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val alwaysGay by setting("Always Gay", "N3mbs, The_Prototype137, femboyfami, xiaoxinxin6666") { page == Page.COMMANDS && gayCommand != CommandMode.DISABLED }
    private val neverGay by setting("Never Gay", "Barackobamar, S1owwalk, Wizard_11") { page == Page.COMMANDS && gayCommand != CommandMode.DISABLED }
    private val blackCommand by setting("Black Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val indianCommand by setting("Indian Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val whiteCommand by setting("White Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val alwaysWhite by setting("Always White", "Wizard_11, Barackobamar") { page == Page.COMMANDS && (blackCommand != CommandMode.DISABLED || indianCommand != CommandMode.DISABLED || whiteCommand != CommandMode.DISABLED) }
    private val alwaysBlack by setting("Always Black", "The_Prototype137, N3mbs, femboyfami, xiaoxinxin6666") { page == Page.COMMANDS && (blackCommand != CommandMode.DISABLED || indianCommand != CommandMode.DISABLED || whiteCommand != CommandMode.DISABLED) }
    private val kitCommand by setting("Kit Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val yesOrNo by setting("Yes/No Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val bestPingCommand by setting("Best Ping Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val worstPingCommand by setting("Worst Ping Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val seedCommand by setting("Seed Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val eightBallCommand by setting("8ball Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val coinFlipCommand by setting("Coinflip Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val diceCommand by setting("Dice Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val ppCommand by setting("PP Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val largePP by setting("Large PP", "Wizard_11, Barackobamar") { ppCommand != CommandMode.DISABLED }
    private val smallPP by setting("Small PP", "The_Prototype137, N3mbs, femboyfami, xiaoxinxin6666") { ppCommand != CommandMode.DISABLED }
    private val iqCommand by setting("IQ Command", CommandMode.DISABLED) { page == Page.COMMANDS }
    private val highIQ by setting("High IQ", "Wizard_11, CompileMarley") { iqCommand != CommandMode.DISABLED }
    private val lowIQ by setting("Low IQ", "The_Prototype137, N3mbs, femboyfami, xiaoxinxin6666") { iqCommand != CommandMode.DISABLED }

    private val respondTimer = TickTimer(TimeUnit.SECONDS)
    private val enabledCommands = ArrayList<Command>()
    private val allCommands: List<Command>

    init {
        allCommands = listOf(
            Command(arrayOf("ping", "serverping"), "ping [player]", { pingCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                runPingCommand(event, playerName, whisper, sender)
            },
            Command(arrayOf("tps"), "tps", { tpsCommand }) { event, _, whisper, sender ->
                sendResponse(event, "The current server TPS is ${String.format("%.2f", TPS.tpsBuffer.average())}/20!", whisper, sender)
            },
            Command(arrayOf("online", "onlineplayers"), "online", { onlineCommand }) { event, _, whisper, sender ->
                runOnlineCommand(event, whisper, sender)
            },
            Command(arrayOf("help", "commands"), "help", { CommandMode.ENABLED }) { event, _, _, sender ->
                runHelpCommand(event, sender)
            },
            Command(arrayOf("insult", "roast"), "insult [player]", { insultCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                sendResponse(event, InsultCommand.getInsult(playerName), whisper, sender)
            },
            Command(arrayOf("distance", "dist"), "distance [player]", { distanceCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                val profile = UUIDManager.getByName(playerName) ?: return@Command
                runDistanceCommand(event, profile, whisper, sender)
            },
            Command(arrayOf("gay"), "gay [player]", { gayCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                val percent = if (alwaysGay.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) 100
                else if (neverGay.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase()) || playerName == mc.player.name) 0
                else Random.nextInt(1, 100)
                sendResponse(event, "$playerName is $percent% gay!", whisper, sender)
            },
            Command(arrayOf("black"), "black [player]", { blackCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                val percent = if (alwaysBlack.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) 100
                else if (alwaysWhite.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase()) || playerName == mc.player.name) 0
                else Random.nextInt(1, 100)
                sendResponse(event, "$playerName is $percent% black!", whisper, sender)
            },
            Command(arrayOf("indian"), "indian [player]", { indianCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                val percent = if (alwaysBlack.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) 100
                else if (alwaysWhite.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase()) || playerName == mc.player.name) 0
                else Random.nextInt(1, 100)
                sendResponse(event, "$playerName is $percent% indian!", whisper, sender)
            },
            Command(arrayOf("white"), "white [player]", { whiteCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                val percent = if (alwaysWhite.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase()) || playerName == mc.player.name) 100
                else if (alwaysBlack.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) 0
                else Random.nextInt(1, 100)
                sendResponse(event, "$playerName is $percent% white!", whisper, sender)
            },
            Command(arrayOf("kit"), "kit [name] [player]", { kitCommand }) { event, args, whisper, sender ->
                if (sender != null) {
                    val kitName = if (args.isNotEmpty()) args[0] else null
                    if (kitName == null) sendResponse(event, "Please specify a kit!", whisper, sender)
                    else sendResponse(event, "$sender has received the kit $kitName!", whisper, sender)
                }
            },
            Command(arrayOf("y/n", "yn", "yesorno"), "y/n", { yesOrNo }) { event, _, whisper, sender ->
                sendResponse(event, listOf("yes", "no").random(), whisper, sender)
            },
            Command(arrayOf("bestping", "bp"), "bestping", { bestPingCommand }) { event, _, whisper, sender ->
                runBestPing(event, whisper, sender)
            },
            Command(arrayOf("worstping", "wp"), "worstping", { worstPingCommand }) { event, _, whisper, sender ->
                runWorstPing(event, whisper, sender)
            },
            Command(arrayOf("seed"), "seed", { seedCommand }) { event, _, whisper, sender ->
                sendResponse(event, "The world seed is -2469096471405889707!", whisper, sender)
            },
            Command(arrayOf("8ball", "magic8ball", "eightball"), "8ball [question]", { eightBallCommand }) { event, args, whisper, sender ->
                if (args.isNotEmpty()) run8BallCommand(event, whisper, sender)
            },
            Command(arrayOf("dice", "diceroll"), "dice", { diceCommand }) { event, _, whisper, sender ->
                sendResponse(event, "You roll a dice and get a ${Random.nextInt(1, 7)}!", whisper, sender)
            },
            Command(arrayOf("coinflip", "flip", "flipacoin"), "coinflip", { coinFlipCommand }) { event, _, whisper, sender ->
                sendResponse(event, "You flip a coin and it lands on ${listOf("heads", "tails").random()}!", whisper, sender)
            },
            Command(arrayOf("pp", "penis", "penissize", "ppsize"), "pp [player]", { ppCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                runPPCommand(event, playerName, whisper, sender)
            },
            Command(arrayOf("iq", "IQ", "whatiq"), "iq [player]", { iqCommand }) { event, args, whisper, sender ->
                val playerName = if (args.isNotEmpty()) args[0] else sender ?: return@Command
                val iq = if (highIQ.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) Random.nextInt(220, 301)
                else if (lowIQ.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) Random.nextInt(1, 41)
                else Random.nextInt(50, 181)
                sendResponse(event, "$playerName's IQ is $iq!", whisper, sender)
            }
        )

        onEnable {
            rebuildEnabledCommands()
        }

        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketChat) return@listener
            val detectionResult = detectCommand(it.packet as SPacketChat)
            if (detectionResult.isCommand && detectionResult.command != null) {
                executeCommand(this, detectionResult)
            }
        }
    }

    private fun rebuildEnabledCommands() {
        enabledCommands.clear()
        allCommands.forEach {
            if (it.modeSetting() != CommandMode.DISABLED) {
                enabledCommands.add(it)
            }
        }
    }

    private fun runPingCommand(event: SafeClientEvent, playerName: String, whisper: Boolean, sender: String?) {
        val profile = UUIDManager.getByName(playerName) ?: return
        val ping = event.connection.getPlayerInfo(profile.uuid)?.responseTime ?: 0
        val form = if (ping == 0) "not calculated by the server yet" else "${ping}ms"
        sendResponse(event, "${if (playerName == event.player.name) "My" else "$playerName's"} ping is $form!", whisper, sender)
    }

    private fun runPPCommand(event: SafeClientEvent, playerName: String, whisper: Boolean, sender: String?) {
        val pp = if (largePP.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase()) || playerName.equals(event.player.name, true)) {
            "8================================D"
        } else if (smallPP.split(",").map { it.trim().lowercase() }.contains(playerName.lowercase())) {
            "8=D"
        } else {
            "8${"=".repeat(Random.nextInt(2, 11))}D"
        }
        sendResponse(event, pp, whisper, sender)
    }

    private fun runBestPing(event: SafeClientEvent, whisper: Boolean, sender: String?) {
        val bestPing = event.connection.playerInfoMap
            .map { it.gameProfile to it.responseTime }
            .filter { it.second != 0 }
            .minByOrNull { it.second } ?: return
        sendResponse(event, "${bestPing.first.name} has the best ping at ${bestPing.second}ms!", whisper, sender)
    }

    private fun runWorstPing(event: SafeClientEvent, whisper: Boolean, sender: String?) {
        val worstPing = event.connection.playerInfoMap
            .map { it.gameProfile to it.responseTime }
            .maxByOrNull { it.second } ?: return
        sendResponse(event, "${worstPing.first.name} has the worst ping at ${worstPing.second}ms!", whisper, sender)
    }

    private fun runOnlineCommand(event: SafeClientEvent, whisper: Boolean, sender: String?) {
        val onlineProfiles = event.connection.playerInfoMap.map { it.gameProfile.name }.take(20)
        sendResponse(event, "Online players: ${onlineProfiles.joinToString(", ")}", whisper, sender)
    }

    private fun run8BallCommand(event: SafeClientEvent, whisper: Boolean, sender: String?) {
        val responses = listOf("It is certain.", "It is decidedly so.", "Without a doubt.", "Yes definitely.", "You may rely on it.", "As I see it, yes.", "Most likely.", "Outlook good.", "Yes.", "Signs point to yes.", "Reply hazy, try again.", "Ask again later.", "Better not tell you now.", "Cannot predict now.", "Concentrate and ask again.", "Don't count on it.", "My reply is no.", "My sources say no.", "Outlook not so good.", "Very doubtful.")
        sendResponse(event, responses.random(), whisper, sender)
    }

    private fun runHelpCommand(event: SafeClientEvent, sender: String?) {
        val commandList = enabledCommands.joinToString(", ") { if (it.usage.isEmpty()) it.names[0] else it.usage }
        val message = "Available commands: $commandList"
        if (helpCommand == HelpCommandMode.SENDCHAT) {
            sendResponse(event, message, false, null)
        } else {
            sendResponse(event, message, true, sender)
        }
    }

    private fun runDistanceCommand(event: SafeClientEvent, profile: PlayerProfile, whisper: Boolean, sender: String?) {
        val visible = event.world.playerEntities.map { it.gameProfile }.any { it.name == profile.name && it.id == profile.uuid }
        if (!visible) {
            sendResponse(event, "I can't see ${profile.name}!", whisper, sender)
        } else {
            val entity = event.world.playerEntities.firstOrNull { it.name == profile.name && it.uniqueID == profile.uuid } ?: return
            val distance = event.player.distanceTo(entity).toInt()
            sendResponse(event, "${profile.name} is $distance blocks away from me!", whisper, sender)
        }
    }

    private fun detectCommand(packet: SPacketChat): CommandDetectionResult {
        val message = packet.chatComponent.unformattedText
        val isWhisper = message.contains(" whispers: ")
        
        if (isWhisper && detectionMode == Detection.CHATONLY) return CommandDetectionResult(false, null, null, false, emptyList())

        val sender = if (isWhisper) {
            message.substringBefore(" whispers:").trim()
        } else if (message.startsWith("<") && message.contains(">")) {
            message.substringAfter("<").substringBefore(">").trim()
        } else {
            return CommandDetectionResult(false, null, null, false, emptyList())
        }
        
        val content = if (isWhisper) message.substringAfter(" whispers: ").trim() else message.substringAfter(">").trim()

        val blacklistedPlayers = blackList.split(",").map { it.trim() }
        val isBlacklisted = blacklistedPlayers.any { it.equals(sender, true) }
        
        if (if (invertBlacklist) !isBlacklisted else isBlacklisted) {
            return CommandDetectionResult(false, null, null, false, emptyList())
        }

        val prefix = detectionPrefixes.split(",").map { it.trim() }.firstOrNull { content.startsWith(it) } ?: return CommandDetectionResult(false, null, sender, isWhisper, emptyList())
        
        val commandText = content.removePrefix(prefix).trim()
        val parts = commandText.split(" ")
        
        if (parts.isEmpty()) return CommandDetectionResult(false, null, sender, isWhisper, emptyList())
        
        val commandName = parts[0].lowercase()
        val args = parts.drop(1)
        
        rebuildEnabledCommands()
        val detectedCommand = enabledCommands.firstOrNull { cmd -> cmd.names.any { it.equals(commandName, true) } }
        
        return CommandDetectionResult(detectedCommand != null, detectedCommand, sender, isWhisper, args)
    }

    private fun executeCommand(event: SafeClientEvent, result: CommandDetectionResult) {
        result.command?.run?.invoke(event, result.args, result.isWhisper, result.sender)
    }

    private fun sendResponse(event: SafeClientEvent, text: String, whisper: Boolean, sender: String?) {
        if (respondTimer.tickAndReset(respondDelay.toLong())) {
            val message = if (whisper && sender != null) "/w $sender [$botName] $text" else "${if (greenText) "> " else ""}[$botName] $text"
            MessageSendUtils.sendServerMessage(event, message)
        }
    }

    private data class CommandDetectionResult(
        val isCommand: Boolean,
        val command: Command?,
        val sender: String?,
        val isWhisper: Boolean,
        val args: List<String>
    )

    class Command(
        val names: Array<String>,
        val usage: String,
        val modeSetting: () -> CommandMode,
        val run: (SafeClientEvent, List<String>, Boolean, String?) -> Unit
    )

    private enum class Page { GENERAL, COMMANDS }
    private enum class Detection(override val displayName: CharSequence) : DisplayEnum { CHATONLY("Chat Only"), CHATANDWHISPERS("Chat And Whispers") }
    private enum class CommandMode(override val displayName: CharSequence) : DisplayEnum { DISABLED("Disabled"), FRIENDONLY("Friend Only"), ENABLED("Enabled") }
    private enum class HelpCommandMode(override val displayName: CharSequence) : DisplayEnum { SENDCHAT("Send In Chat"), SENDMSG("Send In Message") }
}