package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt.concurrentListener
import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.defaultScope
import dev.wizard.meta.util.threads.runSafeSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.network.play.server.SPacketChat
import java.io.File
import java.util.*
import kotlin.random.Random

object AutoBait : Module(
    name = "AutoBait",
    category = Category.MISC,
    description = "Automatically ragebaits"
) {
    private val targetPlayers by setting("Target Players", "N3mbs, The_Prototype137, femboyfami, xiaoxinxin6666")
    private val detectionMode by setting("Detection", Detection.CHATONLY)
    private val respondDelay by setting("Response Delay", 2, 0..10, 1)
    private val greentext by setting("Greentext", false)
    private val responseMode by setting("Response Mode", ResponseMode.BUILTIN)
    private val customBaitFile by setting("Custom Bait File", "custombait.txt") { responseMode == ResponseMode.CUSTOM }
    private val reloadCustomBaits by setting("Reload Custom Baits", true) { responseMode == ResponseMode.CUSTOM }

    private val respondTimer = TickTimer(TimeUnit.SECONDS)
    private val customBaits = LinkedHashMap<String, String>()

    init {
        reloadCustomBaits.listeners.add {
            reloadCustomBaits.value = true
            customBaits.clear()
            loadCustomBaits()
        }

        onEnable {
            reloadCustomBaits.value = true
            customBaits.clear()
            if (responseMode == ResponseMode.CUSTOM || responseMode == ResponseMode.BOTH) {
                loadCustomBaits()
            }
        }

        onDisable {
            reloadCustomBaits.value = true
        }

        concurrentListener<PacketEvent.Receive> { event ->
            runSafeSuspend {
                if (event.packet !is SPacketChat) return@runSafeSuspend
                val rawMessage = event.packet.chatComponent.unformattedText
                val message = rawMessage.lowercase()
                val strippedMessage = stripFormatting(message)

                val isWhisper = rawMessage.contains(" whispers: ")
                if (isWhisper && detectionMode == Detection.CHATONLY) return@runSafeSuspend

                val sender = if (isWhisper) {
                    rawMessage.substringBefore(" whispers:").trim()
                } else if (rawMessage.startsWith("<") && rawMessage.contains(">")) {
                    rawMessage.substringAfter("<").substringBefore(">").trim()
                } else {
                    return@runSafeSuspend
                }

                val targetList = targetPlayers.split(",").map { it.trim().lowercase() }
                if (!targetList.contains(sender.lowercase())) return@runSafeSuspend

                val response = when (responseMode) {
                    ResponseMode.CUSTOM -> getCustomResponse(strippedMessage)
                    ResponseMode.BUILTIN -> getResponse(strippedMessage)
                    ResponseMode.BOTH -> getCustomResponse(strippedMessage) ?: getResponse(strippedMessage)
                }

                if (response != null) {
                    sendResponse(response, isWhisper, sender)
                }
            }
        }
    }

    private fun loadCustomBaits() {
        defaultScope.launch(Dispatchers.IO) {
            val file = File("trollhack", customBaitFile)
            if (!file.exists()) {
                file.createNewFile()
                file.writeText(
                    "# Custom bait responses\n# Format: [trigger] --- [response]\n# Each line is a separate trigger-response pair\n# Examples:\n# ez --- your ez\n# mad --- no u mad\n# skill issue --- your skill issue\n# nn --- who are you\n"
                )
                NoSpamMessage.sendMessage("$chatName Created ${file.name} template!")
                return@launch
            }

            try {
                file.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("---")
                        if (parts.size == 2) {
                            val trigger = parts[0].trim().lowercase()
                            val response = parts[1].trim()
                            if (trigger.isNotEmpty() && response.isNotEmpty()) {
                                customBaits[trigger] = response
                            }
                        }
                    }
                }
                if (customBaits.isNotEmpty()) {
                    NoSpamMessage.sendMessage("$chatName Loaded ${customBaits.size} custom bait responses!")
                }
            } catch (e: Exception) {
                NoSpamMessage.sendError("$chatName Failed loading custom baits: $e")
            }
        }
    }

    private fun getCustomResponse(message: String): String? {
        for ((trigger, response) in customBaits) {
            if (message.contains(trigger)) {
                return response
            }
        }
        return null
    }

    private fun getResponse(message: String): String {
        val lower = message.lowercase()
        val player = mc.player ?: return ""
        val playerName = player.name.lowercase()

        return when {
            lower.contains("i'm not") || lower.contains("im not") -> listOf("erm no?", "erm yes?", "um no? loll", "yes", "no", "copium?", "you are", "you fell off", "touch grass nn", "L + maidenless", "ok and?", "erm... what the spruce?", "no you're not", "yes you are", "source: trust me bro", "you're* eee", "i'm not convinced").random()
            lower.contains(" nn") || lower.endsWith("nn") || lower.contains(" nn ") -> listOf("namemc views?", "namemc searches?", "relevancy?", "who ARE YOU?!?!?!?", "you're talking to me? LOL", "u talking to me?", "no skill no speak nn", "but look at his fake ass jd LOL", "nn = no name? checks out", "nn stands for 'never noticed'", "nn + L", "your kd is nn too?", "nn = no nuts", "nn = no neck", "nn = no notoriety", "nn = no namemc", "nn = no nuts no glory", "namemc 404'd", "nn = no one knows").random()
            lower.contains(" iq") || lower.contains("what is your iq") || lower.contains("what is ur iq") || lower.contains(" iq?") -> listOf(Random.nextInt(200, 300).toString(), "yes", "over 9000", "high enough to own you", "higher than your pop count", "tree fiddy", "69,420", "enough to know you're bad", "420", "\u221e", "higher than your skill", "error: too high", "your iq = room temp").random()
            lower.contains("your doxxed") || lower.contains("your bad") || lower.contains("your coping") || lower.contains("your mad") || lower.contains("your cheating") -> listOf("you're*", "your*", "ur*", "you're* (fixed it for you)", "you're* (again)", "your grammar doxxed itself", "you're* (bad at spelling)", "you're* (L)", "you're* (touch grass)", "you're* (git gud)").random()
            lower.contains(playerName) -> listOf("who me?", "me?", "nn dog talking to me?", "u talkin to me?", "are you talking to me?", "me personally?", "who asked?", "who asked you nn", "i'm him", "you rang?", "yes?", "what now?", "speak nn").random()
            lower.contains("you are") || lower.contains("youre") || lower.contains("you're") -> listOf("who me?", "me?", "nn dog yapping again?", "no u", "no you", "no u NN", "erm... projection?", "you are (fixed)", "you are (correcting you)", "i'm him", "you're projecting", "no i'm not", "yesn't").random()
            Regex("you [a-z]+").containsMatchIn(lower) -> listOf("who me?", "me?", "u talkin to me?", "no u", "ok and?", "fs", "L", "cope", "seethe", "mald", "touch grass", "LEL + grass", "no i'm not", "yes i am", "ok boomer", "and?", "your point?").random()
            lower.contains("main") && (lower.contains("?") || lower.contains("alt") || lower.contains("smurf")) -> listOf("begging to know my main LMFAO", "wouldnt you like to know LOLL", "this is my main", "fitmc", "they call me \"just the tip\"", "my main is your dad", "main? this is my only acc nn", "i only play on smurf (this is smurf)", "main = main pain (you)", "main character syndrome? (you)", "my main is in your walls", "main? i main baiting", "main = maidenless").random()
            lower.contains("im the") || lower.contains("i'm the") -> listOf("Um no?", "erm no?", "LOL SURE", "fsfs", "uh huh", "no you're not", "erm... source?", "i'm the sigma, you're the cuck", "i'm him, you're her (maid)", "i'm the table", "i'm the one who baits", "i'm the real", "i'm the goat", "i'm the one your mom warned you about").random()
            lower.contains("unfunny") || lower.contains("who laughed") || lower.contains("not funny") -> listOf("i laughed", "your mom laughed", "everyone in vc laughed", "we all laughed", "the server laughed", "your dad laughed (then left)", "i laughed so hard i crashed", "chat laughed", "my dog laughed", "the void laughed", "your tears = funny").random()
            lower.contains("i win") || lower.contains("we win") || (lower.contains("won") && lower.contains("i ")) -> listOf("erm no?", "nice cope haha", "cope nn", "fs bro", "ur not tuff twain", "you lost the second you typed", "you lost", "i win by default (you typed)", "gg ez", "get rekt nn", "L + you fell off + grass", "win? you lost", "i winn't", "win trading detected").random()
            lower.contains("ez") || lower.contains("easy") -> listOf("ez for me", "ez clap", "ez pz", "you = ez", "ez nn", "ez when opponent is you", "ez when i try", "ez when you exist").random()
            lower.contains("mad") || lower.contains("angry") || lower.contains("malding") || lower.contains("seething") || lower.contains("mald") -> listOf("mald harder", "seethe more", "cope harder", "touch grass", "mald detected", "malding rn", "seethe + dilate", "cry about it", "tears = fuel", "malding in 4k", "cope + seethe + dilate").random()
            lower.contains(" l ") || lower.contains("take the l") || lower.contains(" l+") -> listOf("you took the L factory", "L + grass", "L + maidenless", "L + nn", "i gave you the L", "L + fatherless + grass", "L + no u", "L + skill issue").random()
            lower.contains("skill issue") || lower.contains("git gud") -> listOf("your skill issue", "skill issue + L", "git gud nn", "skill issue detected", "your hands = skill issue", "skill issue is your middle name", "skill issue = you", "git gud or git rekt").random()
            lower.contains("hacker") || lower.contains("cheater") || lower.contains("cheat") || lower.contains("closet") -> listOf("cope", "seethe", "mald", "skill issue", "git gud", "ez", "your client = free", "cope + seethe", "hacker? no, you're just bad", "cheat? nah, skill diff", "closet = closed skill").random()
            lower.contains("pop") || lower.contains("pops") || lower.contains("count") -> listOf("pop count = 0", "your pops = my yawns", "pop count = cope count", "i popped your ego", "pop count = skill count (0)").random()
            lower.contains("fit") && (lower.contains("?") || lower.contains("drip")) -> listOf("fit = mid", "drip = dry", "fit check failed", "your fit = wish.com", "drip = drip drop (tears)", "fit = fatherless").random()
            lower.contains("cry") || lower.contains("crying") -> listOf("cry harder NN", "cry about it", "cry + cope", "crying rn", "cry in 4k", "tears = my hydration").random()
            else -> listOf("erm no?", "nice cope haha", "cope nn", "i win YOU LOSE.", "LOLOLOLOL", "JAJAJAJA cope", "LOL!!!", "lel", "LEL", "L", "no u", "touch grass", "seethe", "mald", "cope harder", "ez", "gg ez", "get rekt", "fsfs", "ok and?", "who asked?", "L + grass", "your mom", "deez nuts", "L + fatherless", "skill issue", "git gud", "your client = wish.com", "pop count = 0", "namemc = 404", "jd = fake", "nn = no name no fame", "yap yap yap", "bark bark", "woof woof", "meow meow (catboy detected)", "erm... what the spruce?", "cringe", "LMAO", "LMFAO", "ROFL", "L + no u + grass", "you're ngmi", "never gonna make it", "you're mid", "mid detected", "your aura = 0", "aura check failed", "vibe check = failed", "your vibe = off", "you're not him", "i'm him, you're mid", "you're not that guy").random()
        }
    }

    private fun sendResponse(response: String, isWhisper: Boolean, sender: String) {
        if (respondTimer.tickAndReset(respondDelay.toLong())) {
            val formatted = if (greentext) "> $response" else response
            val message = if (isWhisper) "/w $sender $formatted" else formatted
            MessageSendUtils.sendServerMessage(this, message)
        }
    }

    private fun stripFormatting(text: String): String {
        return text.replace(Regex("§[0-9a-fk-or]"), "").replace(Regex("&[0-9a-fk-or]"), "")
    }

    private enum class Detection(val displayName: String) {
        CHATONLY("Chat Only"),
        CHATANDWHISPERS("Chat And Whispers");

        override fun toString(): String = displayName
    }

    private enum class ResponseMode(val displayName: String) {
        BUILTIN("Built-in Responses"),
        CUSTOM("Custom File"),
        BOTH("Both");

        override fun toString(): String = displayName
    }
}
