package dev.wizard.meta.manager.managers

import dev.wizard.meta.event.ListenerKt.safeListener
import dev.wizard.meta.event.SafeClientEvent
import dev.wizard.meta.event.events.ConnectionEvent
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.ModuleManager
import dev.wizard.meta.module.modules.misc.AltProtect
import dev.wizard.meta.util.SecurityUtils
import java.util.*

object MetaManager : Manager() {
    val wizardUUID = UUID.fromString("77f432a7-5e2d-4b28-a4d1-c04fdef2e005")
    var hasAltChecked: Boolean = false
    var hasDisabled: Boolean = false

    fun isDevorfalse(): Boolean {
        val safe = SafeClientEvent.instance ?: return false
        return safe.mc.session.profile.id == wizardUUID
    }

    init {
        safeListener<ConnectionEvent.Connect> {
            if (!SecurityUtils.llIIIIllIIlIIlII()) {
                SecurityUtils.lIIIllIIllIIIIIllII(SecurityUtils.IllIIllIIIIllII.IIllIIIIIIIllllIIIllIIII)
            }
        }

        safeListener<TickEvent.Post> {
            if (AltProtect.autoToggle) {
                if (!hasAltChecked) {
                    AltProtect.checkAccountChange()
                }
            }
            if (!hasDisabled) {
                ModuleManager.modules.filter { it.isDevOnly }.forEach { it.disable() }
                hasDisabled = true
            }
        }

        safeListener<ConnectionEvent.Disconnect> {
            ModuleManager.modules.filter { it.isDevOnly }.forEach { it.disable() }
            hasAltChecked = false
            hasDisabled = false
        }
    }
}
