package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.MessageSendUtils
import kotlin.random.Random

object InsultCommand : ClientCommand("insult", description = "insult a player") {

    fun getInsult(name: String): String {
        val insults = arrayOf(
            "sit {name}, your my dog", "cope harder {name}, still mid", "{name} is my dog", "ez clap {name}, next caller",
            "pop count = 0 {name}, keep crying", "skill issue {name}, git rekt", "nn yapping {name}, who asked?",
            "namemc 404 {name}, never existed", "aura -1000 {name", "{name} got no bitches, fatherless too",
            "popped ego {name}, no totem", "wish.com client {name}, free trial skill", "strafe = random walk {name}",
            "all {name} does is run", "stash = griefed by 12yo {name}", "main = smurf {name}, still trash",
            "stream 0 viewers {name}, bots left", "montage 240p {name}, windows movie maker", "rank default {name}, prefix [NN]",
            "replay mod replaying death {name}", "vpn leaked ip {name}", "dox fake {name}, still scared",
            "mom joined call {name}", "dad left at spawn {name}", "chair squeaks {name}, malding irl",
            "mic hotkey to cry {name}", "webcam off {name}, hiding tears", "setup 2012 {name}, potato pc",
            "wifi 2 bars {name}, lag excuse", "fps 20 {name}, slideshow pvp", "render distance 2 {name}, blind",
            "skin stolen {name}, namemc beggar", "username 2015 {name}, still nn", "uuid random {name}, no history",
            "{name} is a constant victim", "{name} will never be him", "{name} will never be goated",
            "{name} = no mog", "{name} is NOT cooking (as usual)", "hush up {name}, sit down NN"
        )
        return insults.random().replace("{name}", name)
    }

    init {
        player("target") { playerArg ->
            executeSafe {
                val name = getValue(playerArg).name
                MessageSendUtils.sendServerMessage("[InsultCommand] ${getInsult(name)}")
            }
        }
    }
}
