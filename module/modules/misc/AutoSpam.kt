package dev.wizard.meta.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.ListenerKt
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.extension.synchronized
import dev.wizard.meta.util.interfaces.DisplayEnum
import dev.wizard.meta.util.text.ChatTextUtils
import dev.wizard.meta.util.text.MessageDetection
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayList
import java.util.Locale
import kotlin.random.Random

object AutoSpam : Module(
    name = "AutoSpam",
    category = Category.MISC,
    description = "Spammer with multiple modes"
) {
    private val spamMode = setting("Spam Mode", SpamMode.AUTOCOPE)
    private val spammerOrder = setting("Order", Mode.RANDOM_ORDER) { spamMode.value != SpamMode.MSG }
    private val delay = setting("Delay", 10, 1..180, 1, description = "Delay between messages, in seconds")
    private val customFileName = setting("Custom File", "custom.txt") { spamMode.value == SpamMode.CUSTOM }
    private val antiSpamBypass = setting("Bypass Anti-Spam", false)
    private val msgTarget = setting("Target", "PlayerName") { spamMode.value == SpamMode.MSG }
    private val reloadSpammer = setting("Reload Spammer", true)

    private val spammer = ArrayList<String>().synchronized()
    private val timer = TickTimer(TimeUnit.SECONDS)
    private var currentLine = 0
    private val spammerDir = File("trollhack/spammer")
    private val defaultMessages: Map<SpamMode, List<String>>

    init {
        if (!spammerDir.exists()) {
            spammerDir.mkdirs()
            NoSpamMessage.sendMessage(this, "Created spammer directory!")
        }

        defaultMessages = mapOf(
            SpamMode.AUTOCOPE to listOf(
                "cope harder", "imagine coping this hard", "you're literally coping rn",
                "stop coping and get good", "that's a lot of cope for one hit",
                "cope detected, stay mad", "you coped so hard you lagged",
                "keep typing, it fuels me", "cope harder, the logs aren't helping",
                "you're deflecting harder than your aim", "take a screenshot of this cope",
                "is your keyboard okay from all that coping?", "cope so strong it's impressive",
                "massive cope incoming", "your keyboard is 90% excuses",
                "I win, you cope — balance", "drown in your own cope",
                "that's not a strat, it's just coping", "copium"
            ),
            SpamMode.PKBAIT to listOf(
                "LETS GO PK VC LOL", "LETS BLAZE NN", "WHO ARE YOU", "IM IN HYPERION VC",
                "JQQ IS IN VC LETS GO", "JQQ IS IN VC LOL", "WHO ARE YOU",
                "LOL THIS KID KNOWS SO MUCH ABOUT ME", "BUT HE WONT GO INTO VC LEL",
                "ARENT YOU RATTED BY SN0WHOOK LOL", "LEL I HAVE ALL UR LOGS", "SKITTY IS IN VC",
                "I JUST TOLD ETHAN THAT UR TRYING TO BLAZE LOL", "LOL WHO ARE YOU",
                "DIDNT YOU RUN SN0W LOL", "UR HARMLESS", "LEL", "LETS GO BLAZE LELASAUCE",
                "LOL I HEARD YOU RAN KAMI SN0W LOL", "I HEARD UR TRYING TO BLAZE BUT UR GRAILED",
                "LOL UR RATTED AS FUCK", "LOL LETS BLAZE IN HYPERION VC", "LEL YOU NN",
                "WHO ARE YOU", "LETS GO BLAZE IN HYPERION VC", "LETS BLAZE IN PK VC LOL",
                "LETS GO PK VC LOL", "WHO ARE YOU", "COMPARE LUIGIHACK UID", "EZZZZZZ",
                "NO SKILL LELELLEEL", "JAP CLIENT USER", "EZZZZZZZZZ",
                "DIDNT U BEG OXY FOR PICS LOLOLOL", "NO SEXMASTER-CC?", "WHO ARE YOU LMAOOOO",
                "FREE QD LMAOOOO", "U MAD?", "BROS MAD XDDDDDDDDDD", "SEXMASTER-CC OWNS U"
            ),
            SpamMode.WHATIS to listOf(
                "what is this gameplay", "what is this aim", "what is this movement",
                "what is this strategy", "what is this decision making", "what is this positioning",
                "what is this combo attempt", "what is this defense", "what is this ping abuse",
                "what is this walking simulator", "what is this reaction time",
                "what is this hotbar setup", "what is this reach attempt", "what is this mouse control",
                "what is this laggy mess", "what is this fear-based PvP",
                "what is this pack you're using", "what is this glass jaw defense",
                "what is this low effort duel", "what is this excuse for PvP"
            ),
            SpamMode.BULLY to listOf(
                "you're actually so bad", "uninstall the game", "go back to creative mode",
                "you shouldn't be playing this", "embarrassing gameplay", "did you even try?",
                "bro plays like a bot", "get good, seriously", "this is painful to watch",
                "was that supposed to be a combo?", "0 braincells were used", "you're just free kills",
                "you dropped faster than my fps", "get rolled, noob", "does your mouse even work?",
                "touch grass and uninstall", "I've seen villagers PvP better",
                "you play like it's your first day", "this ain't creative mode, clown",
                "imagine losing that hard", "you move like a fridge",
                "PvPing you is like fighting a training dummy", "are you lagging or just bad?",
                "you got outplayed by someone half-asleep", "if bad was a crime, you'd get life",
                "I beat you with one hand, literally", "it's like you're trying to lose",
                "stop before you embarrass yourself more", "I thought this was gonna be a challenge"
            ),
            SpamMode.EGOSPAM to listOf(
                "I'M JUST BUILT DIFFERENT", "NONE OF YOU ARE ON MY LEVEL",
                "I'M THE STANDARD YOU'RE CHASING", "I DON'T HAVE TO TRY",
                "THIS IS WHAT PEAK LOOKS LIKE", "I'M PLAYING A DIFFERENT GAME",
                "EVERYONE HERE IS MID", "I'M JUST NATURALLY BETTER",
                "THIS SERVER REVOLVES AROUND ME", "I WAKE UP BETTER THAN YOU",
                "YOU'RE ALL JUST FILLER", "EVERYONE HERE IS A BACKGROUND CHARACTER",
                "I'M WHAT 'GOOD' LOOKS LIKE", "I COULD DO THIS BLINDFOLDED",
                "I'M IN A LEAGUE OF MY OWN", "YOU GUYS ARE JUST CONTENT FOR ME",
                "EVERYONE HERE IS A WARMUP", "I LOG IN = I WIN", "THIS IS EFFORTLESS FOR ME",
                "I'M THE REASON STATS EXIST", "I'M THE PLAYER PEOPLE TRY TO BE",
                "EVERYONE'S LOOKING UP AT ME", "I'M THE BENCHMARK AROUND HERE",
                "I'M THE ONLY NAME THAT MATTERS", "I'M THAT GUY — YOU KNOW IT",
                "YOU CAN'T IGNORE ME, EVEN IF YOU TRIED", "I'M WINNING WHILE CHILLING",
                "I'M TOP TIER WITHOUT EVEN QUEUING", "I AM THE RANKED LADDER",
                "I DON'T SWEAT, I SET RECORDS", "PEOPLE WATCH ME TO LEARN",
                "MY MOVES GET CLIPPED, YOURS GET FORGOTTEN",
                "I DON'T IMPROVE — I JUST DISPLAY PERFECTION", "YOU'RE COMPETING, I'M DEMONSTRATING",
                "I'M NOT IN THE LOBBY, I'M ABOVE IT", "I'M JUST SHOWING OFF AT THIS POINT",
                "EVERY FIGHT IS A HIGHLIGHT REEL FOR ME", "I DON'T TRAIN, I TEACH",
                "I'M NOT HERE TO WIN — I'M HERE TO REMIND YOU",
                "I COULD BE AFK AND STILL BE TOP FRAG", "MY EXISTENCE IS A BUFF",
                "I JOINED AND THE SERVER UPGRADED", "I'M NOT OP — I'M THE BASELINE",
                "MY PRESENCE IS THE BETA", "I DON'T QUEUE, I ARRIVE", "I WAS BORN QUEUED UP",
                "YOUR BEST DAY IS MY DEFAULT", "EVEN THE SERVER KNOWS MY NAME",
                "EVERYONE HERE'S JUST AN AUDIENCE", "I'M WHAT YOU WISH YOU WERE",
                "I DON'T TRY, I JUST WIN", "I'M THE DIFFERENCE MAKER",
                "YOU'RE SEEING EXCELLENCE IN MOTION", "I PLAY THE GAME — OTHERS CHASE IT",
                "MY DEFAULT IS YOUR PEAK", "I AM THE HIGHLIGHT",
                "EVERY SERVER I JOIN BECOMES MINE", "I'M ALREADY KNOWN WITHOUT SPEAKING",
                "THIS IS NATURAL TO ME", "I'M A WALKING W"
            )
        )

        onEnable {
            reloadSpammer.value = true
            spammer.clear()
            if (spamMode.value == SpamMode.MSG) {
                NoSpamMessage.sendMessage(this, "MSG mode enabled - sending to ${msgTarget.value}")
            } else {
                DefaultScope.launch(Dispatchers.IO) {
                    val currentFile = getCurrentFile()
                    if (currentFile == null) {
                        NoSpamMessage.sendError(this@AutoSpam, "No file specified!")
                        disable()
                        return@launch
                    }

                    if (spamMode.value != SpamMode.CUSTOM) {
                        createFileWithDefaults(currentFile, spamMode.value)
                    } else if (!currentFile.exists()) {
                        NoSpamMessage.sendError(this@AutoSpam, "Custom file ${customFileName.value} not found! Creating template...")
                        createFileWithDefaults(currentFile, SpamMode.CUSTOM)
                    }

                    try {
                        currentFile.forEachLine { 
                            if (it.isNotBlank()) spammer.add(it.trim()) 
                        }
                        val displayName = if (spamMode.value == SpamMode.CUSTOM) customFileName.value else spamMode.value.name
                        NoSpamMessage.sendMessage(this@AutoSpam, "Loaded ${spammer.size} messages from $displayName!")
                        if (spammer.isEmpty()) {
                            NoSpamMessage.sendError(this@AutoSpam, "No valid messages found in $displayName!")
                            disable()
                        }
                    } catch (e: Exception) {
                        val displayName = if (spamMode.value == SpamMode.CUSTOM) customFileName.value else spamMode.value.fileName ?: "Unknown"
                        NoSpamMessage.sendError(this@AutoSpam, "Failed loading $displayName, $e")
                        disable()
                    }
                }
            }
        }

        spamMode.listeners.add {
            if (isEnabled) {
                disable()
                enable()
            }
        }

        reloadSpammer.listeners.add {
            reloadSpammer.value = true
            if (isEnabled) {
                disable()
                enable()
            }
        }

        customFileName.listeners.add {
            if (isEnabled && spamMode.value == SpamMode.CUSTOM) {
                disable()
                enable()
            }
        }

        ListenerKt.listener<TickEvent.Post> {
            if (!timer.tickAndReset(delay.value)) return@listener

            if (spamMode.value == SpamMode.MSG) {
                val message = if (spammerOrder.value == Mode.IN_ORDER) getOrdered() else getRandom()
                if (MessageDetection.Command.TROLL_HACK.detect(message)) {
                    MessageSendUtils.sendTrollCommand(message)
                } else {
                    MessageSendUtils.sendServerMessage(this, "/msg ${msgTarget.value} $message ${getAntiSpam()}")
                }
            } else {
                if (spammer.isEmpty()) return@listener
                val message = if (spammerOrder.value == Mode.IN_ORDER) getOrdered() else getRandom()
                if (MessageDetection.Command.TROLL_HACK.detect(message)) {
                    MessageSendUtils.sendTrollCommand(message)
                } else {
                    MessageSendUtils.sendServerMessage(this, "$message ${getAntiSpam()}")
                }
            }
        }
    }

    private fun getCurrentFile(): File? {
        return when (spamMode.value) {
            SpamMode.CUSTOM -> File(spammerDir, customFileName.value)
            SpamMode.MSG -> null
            else -> spamMode.value.fileName?.let { File(spammerDir, it) }
        }
    }

    private fun createFileWithDefaults(file: File, mode: SpamMode) {
        if (!file.exists()) {
            file.createNewFile()
            val list = defaultMessages[mode] ?: listOf("Add your custom messages here!", "One message per line")
            file.writeText(list.joinToString("\n"))
            NoSpamMessage.sendMessage(this, "Created ${file.name} with default messages!")
        }
    }

    private fun getOrdered(): String {
        currentLine %= spammer.size
        return spammer[currentLine++]
    }

    private fun getRandom(): String {
        if (spammer.size == 1) return spammer[0]
        var prevLine = currentLine
        while (currentLine == prevLine) {
            currentLine = Random.nextInt(spammer.size)
        }
        return spammer[currentLine]
    }

    private fun getAntiSpam(): String {
        return if (antiSpamBypass.value) "[${ChatTextUtils.generateRandomSuffix(3)}]" else ""
    }

    private enum class Mode(override val displayName: CharSequence) : DisplayEnum {
        IN_ORDER("In Order"),
        RANDOM_ORDER("Random Order")
    }

    private enum class SpamMode(override val displayName: CharSequence, val fileName: String?) : DisplayEnum {
        AUTOCOPE("Auto Cope", "autocope.txt"),
        PKBAIT("PK Bait", "pkbait.txt"),
        WHATIS("What Is", "whatis.txt"),
        BULLY("Bully", "bully.txt"),
        EGOSPAM("Ego Spam", "egospam.txt"),
        CUSTOM("Custom", null),
        MSG("Message", null)
    }
}