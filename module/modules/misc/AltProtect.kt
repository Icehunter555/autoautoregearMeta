package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.command.commands.SexDupeCommand
import dev.wizard.meta.module.AbstractModule
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.util.threads.runSafe
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.util.ResourceLocation
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.UUID

object AltProtect : Module(
    name = "AltProtect",
    category = Category.MISC,
    description = "for me"
) {
    private val nameProtect by setting("Name Protect", false)
    val fakeName by setting("Fake Name", "fitmc") { nameProtect }
    private val skinProtect by setting("Skin Protect", false)
    private val disableModules by setting("Disable Revealing Modules", true)
    private val enableModules by setting("Re-enable Revealing", true) { disableModules }
    val autoToggle by setting("Auto Toggle on Alt", true)

    private val disabledByAltProtect = LinkedHashSet<Module>()
    private var initialUUID: UUID? = null
    private var initialName: String? = null

    init {
        onEnable {
            disabledByAltProtect.clear()
            if (disableModules) {
                disableM(AutoCope)
                disableM(MessageModifier)
                disableM(AutoEz)
                disableM(SkinBlinker)
                disableM(Announcer)
                SexDupeCommand.stop()
                disableM(AutoReply)
            }
        }

        onDisable {
            if (enableModules && disabledByAltProtect.isNotEmpty()) {
                disabledByAltProtect.forEach {
                    if (!it.isEnabled) it.enable()
                }
            }
            disabledByAltProtect.clear()
        }
    }

    private fun disableM(module: Module) {
        if (module.isEnabled) {
            module.disable()
            disabledByAltProtect.add(module)
        }
    }

    @JvmStatic
    fun getCurrentName(): String {
        return runSafe {
            if (mc.connection != null || mc.isIntegratedServerRunning) {
                player.gameProfile.name
            } else {
                "sgksgsdfkgjdkfghowr398fheu29u892"
            }
        } ?: "sgksgsdfkgjdkfghowr398fheu29u892"
    }

    @JvmStatic
    fun handleSkin(pl: AbstractClientPlayer, ci: CallbackInfoReturnable<ResourceLocation>) {
        if (pl == mc.player) {
            ci.returnValue = ResourceLocation("minecraft", "textures/entity/steve.png")
        }
    }

    @JvmStatic
    fun handleName(string: String): String {
        if (!nameProtect) return string
        return string.replace(getCurrentName(), fakeName.value)
    }

    @JvmStatic
    fun storeInitialAccount() {
        runSafe {
            initialUUID = mc.session.profile.id
            initialName = mc.session.profile.name
        }
    }

    fun checkAccountChange() {
        runSafe {
            val currentUUID = mc.session.profile.id
            val currentName = mc.session.profile.name
            
            if (initialUUID == null || initialName == null) {
                initialUUID = currentUUID
                initialName = currentName
                return@runSafe
            }

            val accountChanged = currentUUID != initialUUID || currentName != initialName

            if (accountChanged && !isEnabled) {
                enable()
            } else if (!accountChanged && isEnabled) {
                disable()
            }
        }
    }
}
