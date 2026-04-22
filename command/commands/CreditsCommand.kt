package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.util.text.TextFormatting

object CreditsCommand : ClientCommand("credits", description = "show the credits") {

    private fun sendCreditsMessage(name: String, description: String, prio: Int) {
        val nameFormatting = when (prio) {
            1 -> TextFormatting.LIGHT_PURPLE
            2, 3 -> TextFormatting.GOLD
            4 -> TextFormatting.GRAY
            else -> TextFormatting.DARK_GRAY
        }
        val doBold = prio == 1 || prio == 2
        val nameString = "${TextFormatting.RESET}$nameFormatting${if (doBold) TextFormatting.BOLD else ""}$name ${TextFormatting.RESET}"

        val descriptionStringFormatting = when (prio) {
            1, 2, 3 -> TextFormatting.WHITE
            4 -> TextFormatting.GRAY
            else -> TextFormatting.DARK_GRAY
        }
        val descriptionString = "${TextFormatting.RESET}$descriptionStringFormatting${if (prio != 1) TextFormatting.ITALIC else ""}$description${TextFormatting.RESET}"

        NoSpamMessage.sendRaw("", false)
        NoSpamMessage.sendRaw("   $nameString ${TextFormatting.WHITE}${TextFormatting.BOLD}- $descriptionString", false)
    }

    init {
        executeSafe {
            NoSpamMessage.sendRaw("${TextFormatting.BOLD}${TextFormatting.UNDERLINE}Credits: ${TextFormatting.RESET}", false)
            sendCreditsMessage("luna5ama", "creating the original trollhack and putting in a ton of effort into the client", 1)
            sendCreditsMessage("Wizard_11", "making meta!", 1)
            sendCreditsMessage("CompileMarley", "made the highway filler and autopearl module, and provided plenty of ideas for the client", 2)
            sendCreditsMessage("Auoggi", "Working on autoregear and adding the item swapping system", 2)
            sendCreditsMessage("Darki", "Making konas client, which I used to teach myself mixins", 2)
            sendCreditsMessage("ChatGPT", "Making ennui, which was trash but had good ideas", 2)
            sendCreditsMessage("Tkoq", "The number 1 bugtester of trolled, helped with feature suggestions", 3)
            sendCreditsMessage("3uer", "3uer pookie :D suggested many features for the client, including this command!", 3)
            sendCreditsMessage("Berry_07", "My current number 1 bugtester, helped find some bugs", 3)
            sendCreditsMessage("All Trolled Users", "Helped me explore the mod development space and learn", 4)
            sendCreditsMessage("All Current Meta Users", "Their continued support and assistance with both ideas and bugs have helped shape this mod into what it is", 4)
        }
    }
}
