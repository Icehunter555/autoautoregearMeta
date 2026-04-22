package dev.wizard.meta.module.modules.client

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.util.text.TextFormatting
import kotlin.random.Random

object MainMenu : Module(
    "MainMenu",
    category = Category.CLIENT,
    description = "Main Menu Settings",
    alwaysEnabled = true
) {
    val mainMenuKeybinds by setting(this, BooleanSetting(settingName("Main Menu Keybinds"), true))
    val noRealmsButton by setting(this, BooleanSetting(settingName("No Realms Button"), true))
    val customSplash by setting(this, BooleanSetting(settingName("Custom Splash Text"), true))
    val addText = false

    @JvmStatic
    fun sendCustomSplash(): String {
        val splashes = arrayOf(
            "Welcome to Meta 3!",
            "Made with <3 by Wizard_11",
            "Meta Client 0.3!",
            "Thanks, 3uer!",
            "Hopefully probably might fix some crashes!",
            "Thanks, Compile!",
            "Thanks, Berry!",
            "Meta still owns!",
            "${TextFormatting.BOLD}Thanks, Luna!${TextFormatting.RESET}",
            "\"i didnt died, i offhand died\" - Posix"
        )
        return splashes.random()
    }
}
