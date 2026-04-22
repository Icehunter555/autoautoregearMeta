package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.event.ListenerKt
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.event.events.combat.TotemPopEvent
import dev.wizard.meta.manager.managers.FriendManager
import dev.wizard.meta.manager.managers.LogoutManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.misc.FakePlayer
import dev.wizard.meta.util.EntityUtils
import dev.wizard.meta.util.extension.synchronized
import dev.wizard.meta.util.math.vector.distanceSqTo
import dev.wizard.meta.util.text.MessageSendUtils
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.entity.player.EntityPlayer
import java.io.File
import java.util.WeakHashMap

object AutoEz : Module(
    name = "AutoEz",
    category = Category.MISC,
    description = "too ez"
) {
    private val greentext = setting("Greentext", false)
    private val friends = setting("Friends", false)
    private val deathMessages = setting("Death Messages", true)
    private val popMessages = setting("Pop Messages", true)
    private val popDelay = setting("Pop Delay ", 1500, 0..10000, 100) { popMessages.value }
    private val reloadMessages = setting("Reload Messages", true)
    private val popFileName = setting("Pop File", "totem_messages.txt")
    private val deathFileName = setting("Death File", "death_messages.txt")

    private val messagesDir = File("trollhack/messages")
    private var messages = emptyList<String>()
    private var deathMessagesList = emptyList<String>()
    private val attackedPlayers = WeakHashMap<EntityPlayer, PlayerData>().synchronized()
    private var lastPopMessageTime = 0L

    private val defaultPopMessages = listOf(
        "helen keller is better at this game than you, {name}! EZ {count} pop{s}",
        "I almost feel bad for {name}... Just kidding! {count} pop{s}!",
        "Your lack of skill bores me {name}. {count} pop{s}",
        "I expected more of you, {name}! {count} pop{s}!",
        "Just /kill at this point, you popped {count} time{s}.",
        "Is there something wrong with your config? You popped {count} time{s}.",
        "{name} popped faster than bubble wrap under a fat guy. EZ {count} POP{s}!",
        "{name} really though this was a lore smp. EZ {count} POP{s}!",
        "That wasn't a fight, {name}, that was a tutorial. {count} pop{s}",
        "{name} just donated {count} totem{s} to the cause",
        "I just used {name} as a training dummy. {count} pop{s}!",
        "{name} hitting new lows in PvP history. {count} pop{s}",
        "{count} pop{s}, and not one person cared enough to save you, {name}",
        "I've seen less suffering in a war crime tribunal! {count} pop{s}, {name}",
        "{name} popped so much I thought it was a school shooting! {count} pop{s}",
        "{name}'s skill issues make terminal illness look like a buff. {count} pop{s}!",
        "{name} has the survival rate of a hamster in a microwave. {count} pop{s}!",
        "Your skill level makes Darwin proud. {count} pop{s}, {name}!",
        "{name} handles pressure like a 2008 stockbroker. {count} pop{s}",
        "You're the reason your bloodline ends, {name}! {count} pop{s}!",
        "{name} dies more often than hope in a foster home. {count} pop{s}!",
        "That wasn't a pop — that was a eulogy. {count} time{s}, {name}!",
        "You're not a player, {name}. You're a case study in failure. {count} pop{s}!",
        "You're not dying anymore, {name}. You're decaying. {count} pop{s}!",
        "God killed your game, {name}. {count} pop{s} was your crash log.",
        "That wasn't a battle. That was divine punishment. {count} pop{s}, {name}!",
        "You pop like a Columbine yearbook. {count} pop{s}, {name}!",
        "That wasn't PvP. That was a domestic incident. ez {count} pop{s}, {name}!",
        "{name} fights like their parents wanted the miscarriage. {count} pop{s}!"
    )

    private val defaultDeathMessages = listOf(
        "{name} died the way they lived — unprepared and unwanted.",
        "{name} returned to the void that raised them.",
        "{name} finally found something stronger than their denial.",
        "{name} died wishing this was the first time it hurt.",
        "{name} died — not suddenly, but inevitably.",
        "{name} met the same end as every broken thing.",
        "{name} fell faster than the Twin Towers",
        "{name} is the punchline to a joke their family never finished",
        "{name}, you bring new meaning to the term 'quick drop'",
        "You're the human version of a participation trophy, {name}.",
        "Even {name}'s shadow is ashamed to follow them.",
        "{name} is the reason the tutorial exists.",
        "{name}'s skill peaked at 'once upon a time.'",
        "{name} only exists to lower the average.",
        "{name} is the failure everyone else warns about but never expects.",
        "{name} brings new meaning to the word 'pathetic.'",
        "{name} is the embodiment of 'too far gone.'",
        "{name} is a walking condom ad.",
        "{name} is the reason abortions were invented.",
        "{name} is a liability in every single pixel."
    )

    init {
        if (!messagesDir.exists()) messagesDir.mkdirs()

        onEnable {
            loadPopMessages()
            loadDeathMessages()
            reloadMessages.value = true
        }

        popFileName.listeners.add { if (isEnabled) loadPopMessages() }
        deathFileName.listeners.add { if (isEnabled) loadDeathMessages() }
        reloadMessages.listeners.add {
            reloadMessages.value = true
            loadPopMessages()
            loadDeathMessages()
        }

        ListenerKt.listener<TotemPopEvent.Pop> {
            if (it.name != mc.player.name && popMessages.value) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastPopMessageTime >= popDelay.value) {
                    val popMessage = getRandomPopMessage(it.name, it.count)
                    if (friends.value && FriendManager.isFriend(it.name)) {
                        val msg = "My friend ${it.name} popped ${it.count} totem${if (it.count == 1) "!" else "s!"}"
                        MessageSendUtils.sendServerMessage(this, if (greentext.value) "> $msg" else msg)
                    } else if (!FriendManager.isFriend(it.name)) {
                        MessageSendUtils.sendServerMessage(this, if (greentext.value) "> $popMessage" else popMessage)
                    }
                    lastPopMessageTime = currentTime
                }
            }
        }

        ListenerKt.listener<TotemPopEvent.Death> {
            if (it.name != mc.player.name && deathMessages.value) {
                val message = getRandomDeathMessage(it.name)
                if (friends.value && FriendManager.isFriend(it.name)) {
                    val msg = "My friend ${it.name} died after popping ${it.count} totem${if (it.count == 1) "!" else "s!"}"
                    MessageSendUtils.sendServerMessage(this, if (greentext.value) "> $msg" else msg)
                } else if (!FriendManager.isFriend(it.name)) {
                    MessageSendUtils.sendServerMessage(this, if (greentext.value) "> $message" else message)
                }
            }
        }

        ListenerKt.listener<TickEvent.Post> {
            if (deathMessages.value) {
                updateAttackedPlayer()
                removeInvalidPlayers()
                sendDeathMessage()
            }
        }
    }

    private fun getPopFile() = File(messagesDir, popFileName.value)
    private fun getDeathFile() = File(messagesDir, deathFileName.value)

    private fun loadPopMessages() {
        DefaultScope.launch(Dispatchers.IO) {
            val popFile = getPopFile()
            try {
                if (!popFile.exists()) {
                    popFile.createNewFile()
                    popFile.writeText(defaultPopMessages.joinToString("\n"))
                    NoSpamMessage.sendMessage(this@AutoEz, "Created ${popFile.name} with default messages!")
                }
                messages = popFile.readLines().filter { it.isNotBlank() }
                NoSpamMessage.sendMessage(this@AutoEz, "Loaded ${messages.size} totem pop messages from ${popFile.name}!")
            } catch (e: Exception) {
                NoSpamMessage.sendError(this@AutoEz, "Failed to load ${popFile.name}: $e")
                messages = defaultPopMessages
            }
        }
    }

    private fun loadDeathMessages() {
        DefaultScope.launch(Dispatchers.IO) {
            val deathFile = getDeathFile()
            try {
                if (!deathFile.exists()) {
                    deathFile.createNewFile()
                    deathFile.writeText(defaultDeathMessages.joinToString("\n"))
                    NoSpamMessage.sendMessage(this@AutoEz, "Created ${deathFile.name} with default messages!")
                }
                deathMessagesList = deathFile.readLines().filter { it.isNotBlank() }
                NoSpamMessage.sendMessage(this@AutoEz, "Loaded ${deathMessagesList.size} death messages from ${deathFile.name}!")
            } catch (e: Exception) {
                NoSpamMessage.sendError(this@AutoEz, "Failed to load ${deathFile.name}: $e")
                deathMessagesList = defaultDeathMessages
            }
        }
    }

    private fun getRandomPopMessage(name: String, count: Int): String {
        val s = if (count > 1) "s" else ""
        val message = if (messages.isNotEmpty()) messages.random() else "EZZZZZ POP, {name}!  {count} pop{s}!"
        return message.replace("{name}", name).replace("{count}", count.toString()).replace("{s}", s)
    }

    private fun getRandomDeathMessage(name: String): String {
        val message = if (deathMessagesList.isNotEmpty()) deathMessagesList.random() else "RIP {name}! EZ kill!"
        return message.replace("{name}", name)
    }

    private fun updateAttackedPlayer() {
        val attacked = mc.player.lastAttackedEntity
        if (attacked is EntityPlayer && attacked.isEntityAlive && !EntityUtils.isFakeOrSelf(attacked)) {
            val currentData = attackedPlayers[attacked]
            val connectionInfo = mc.connection?.getPlayerInfo(attacked.uniqueID)
            val disconnectTime = if (connectionInfo == null) currentData?.disconnectTime ?: System.currentTimeMillis() else null
            attackedPlayers[attacked] = PlayerData(mc.player.ticksExisted, disconnectTime)
        }
    }

    private fun removeInvalidPlayers() {
        val removeTime = mc.player.ticksExisted - 100
        val currentTime = System.currentTimeMillis()
        val twentyMinutesInMillis = 1200000L
        attackedPlayers.entries.removeIf { (player, data) ->
            if (data.attackTime < removeTime) return@removeIf true
            if (!LogoutManager.isOnline(player.uniqueID) && player.name == FakePlayer.getHudInfo()) return@removeIf true
            !LogoutManager.isOnline(player.uniqueID) && data.disconnectTime != null && (currentTime - data.disconnectTime) > twentyMinutesInMillis
        }
    }

    private fun sendDeathMessage() {
        val deadPlayer = attackedPlayers.keys.find { 
            !it.isEntityAlive && mc.player.distanceSqTo(it) <= 256.0 && !isPlayerNaked(it)
        }
        
        if (deadPlayer != null) {
            attackedPlayers.remove(deadPlayer)
            val message = getRandomDeathMessage(deadPlayer.name)
            if (!FriendManager.isFriend(deadPlayer.name)) {
                val msg = if (greentext.value) "> $message" else message
                MessageSendUtils.sendServerMessage(this, msg)
            }
        }
    }

    private fun isPlayerNaked(player: EntityPlayer): Boolean {
        return player.armorInventoryList.none { !it.isEmpty }
    }

    private data class PlayerData(val attackTime: Int, val disconnectTime: Long?)
}
