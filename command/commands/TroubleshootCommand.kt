package dev.wizard.meta.command.commands

import dev.wizard.meta.command.ClientCommand
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.util.text.NoSpamMessage
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraftforge.common.ForgeVersion
import java.util.*

object TroubleshootCommand : ClientCommand("troubleshoot", arrayOf("debuginfo", "devinfo"), "displays info for troubleshooting") {

    init {
        execute("Print troubleshooting information") {
            NoSpamMessage.sendRaw("Send a screenshot of all information below this line!", false)
            val enabledModules = ModuleManager.modules.filter { it.isEnabled }.joinToString { it.name }
            NoSpamMessage.sendRaw("Enabled Modules:\n$enabledModules", false)
            NoSpamMessage.sendRaw("Meta 0.3B-10mq29", false)
            NoSpamMessage.sendRaw("Forge ${ForgeVersion.getMajorVersion()}.${ForgeVersion.getMinorVersion()}.${ForgeVersion.getRevisionVersion()}.${ForgeVersion.getBuildVersion()}", false)

            val osName = System.getProperty("os.name").lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            NoSpamMessage.sendRaw("Operating System: $osName ${System.getProperty("os.version")} ", false)
            NoSpamMessage.sendRaw("JVM: ${System.getProperty("java.version")} ${System.getProperty("java.vendor")}", false)
            NoSpamMessage.sendRaw("GPU: ${GlStateManager.glGetString(7936)}", false)
            NoSpamMessage.sendRaw("CPU: ${System.getProperty("os.arch")} ${OpenGlHelper.getCpu()}", false)
            NoSpamMessage.sendRaw("Please send a screenshot of the full output to Wizard_11!", false)
        }
    }
}
