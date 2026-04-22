package dev.wizard.meta.gui.mc

import dev.wizard.meta.command.CommandManager
import dev.wizard.meta.command.args.AutoComplete
import dev.wizard.meta.command.args.GreedyStringArg
import dev.wizard.meta.graphics.RenderUtils2D
import dev.wizard.meta.module.modules.client.ClickGUI
import dev.wizard.meta.util.accessor.historyBuffer
import dev.wizard.meta.util.accessor.sentHistoryCursor
import dev.wizard.meta.util.threads.ConcurrentScope
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard

class TrollGuiChat(
    startStringIn: String,
    private val historyBufferIn: String? = null,
    private val sentHistoryCursorIn: Int? = null
) : GuiChat(startStringIn) {

    private var predictString = ""
    private var cachePredict = ""
    private var canAutoComplete = false

    override fun initGui() {
        super.initGui()
        historyBufferIn?.let { historyBuffer = it }
        sentHistoryCursorIn?.let { sentHistoryCursor = it }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (guiChatKeyTyped(typedChar, keyCode)) return

        val text = inputField.text
        if (!text.startsWith(CommandManager.prefix)) {
            displayNormalChatGUI()
            return
        }

        if (canAutoComplete && keyCode == Keyboard.KEY_TAB && predictString.isNotBlank()) {
            inputField.text = inputField.text + predictString
            predictString = ""
        }

        ConcurrentScope.launch {
            cachePredict = ""
            canAutoComplete = false
            autoComplete()
            predictString = cachePredict
        }
    }

    private fun guiChatKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        return when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                mc.displayGuiScreen(null)
                true
            }
            Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> {
                val text = inputField.text.trim()
                if (text.isNotEmpty()) {
                    sendChatMessage(text)
                }
                mc.ingameGUI.chatGUI.addToSentMessages(text)
                mc.displayGuiScreen(null)
                true
            }
            Keyboard.KEY_UP -> {
                getSentHistory(-1)
                false
            }
            Keyboard.KEY_DOWN -> {
                getSentHistory(1)
                false
            }
            Keyboard.KEY_PRIOR -> {
                mc.ingameGUI.chatGUI.scroll(mc.ingameGUI.chatGUI.lineCount - 1)
                false
            }
            Keyboard.KEY_NEXT -> {
                mc.ingameGUI.chatGUI.scroll(-mc.ingameGUI.chatGUI.lineCount + 1)
                false
            }
            else -> {
                inputField.textboxKeyTyped(typedChar, keyCode)
                false
            }
        }
    }

    private fun displayNormalChatGUI() {
        val chat = GuiChat(inputField.text)
        mc.displayGuiScreen(chat)
        chat.historyBuffer = this.historyBuffer
        chat.sentHistoryCursor = this.sentHistoryCursor
    }

    private suspend fun autoComplete() {
        val fullText = inputField.text
        val string = fullText.removePrefix(CommandManager.prefix)
        val parsedArgs = runCatching { CommandManager.parseArguments(string) }.getOrNull() ?: return
        var argCount = parsedArgs.size - 1
        val inputName = parsedArgs[0]

        if (string.endsWith(' ') && !string.endsWith("  ")) {
            argCount++
        }

        if (argCount == 0) {
            commandAutoComplete(inputName)
            return
        }

        val ignoredStringArg = getArgTypeForAtIndex(parsedArgs, argCount, true)
        val withStringArg = getArgTypeForAtIndex(parsedArgs, argCount, false)

        val args = ignoredStringArg ?: withStringArg ?: return
        val both = if (ignoredStringArg != null && withStringArg != null) ignoredStringArg + withStringArg else args
        val inputString = parsedArgs.getOrNull(argCount)

        if (inputString.isNullOrEmpty()) {
            if (args.isNotEmpty()) {
                cachePredict = both.distinct().joinToString("/")
            }
            return
        }

        for (arg in args) {
            if (arg is AutoComplete) {
                arg.completeForInput(inputString)?.let { result ->
                    cachePredict = result.substring(inputString.length.coerceAtMost(result.length))
                    canAutoComplete = true
                    return
                }
            }
        }
    }

    private fun commandAutoComplete(inputName: String) {
        CommandManager.commands.asSequence()
            .flatMap { it.allNames.asSequence().map { name -> name.toString() } }
            .filter { it.length >= inputName.length }
            .filter { it.startsWith(inputName, ignoreCase = true) }
            .minByOrNull { it.length }
            ?.let {
                cachePredict = it.substring(inputName.length.coerceAtMost(it.length))
                canAutoComplete = true
            }
    }

    private suspend fun getArgTypeForAtIndex(parsedArgs: Array<String>, argIndex: Int, ignoreStringArg: Boolean): List<dev.wizard.meta.command.args.AbstractArg<*>>? {
        val command = CommandManager.getCommandOrNull(parsedArgs[0]) ?: return null
        val treeMatchedCounts = command.finalArgs.mapNotNull {
            if (ignoreStringArg && it.argTree.getOrNull(argIndex) is GreedyStringArg) null
            else it.countArgs(parsedArgs) to it
        }

        val maxMatches = treeMatchedCounts.maxOfOrNull { it.first } ?: return null
        return treeMatchedCounts.filter { it.first == maxMatches }.mapNotNull { it.second.argTree.getOrNull(argIndex) }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        RenderUtils2D.drawCapsuleRectFilled(2.0f, height - 14.0f, width - 2.0f, height - 2.0f, ClickGUI.backGround)
        if (predictString.isNotBlank()) {
            val posX = fontRenderer.getStringWidth(inputField.text) + inputField.x
            val posY = inputField.y.toFloat()
            fontRenderer.drawStringWithShadow(predictString, posX.toFloat(), posY, 0x808080)
        }
        inputField.drawTextBox()
    }
}
