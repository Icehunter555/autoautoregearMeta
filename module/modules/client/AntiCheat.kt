package dev.wizard.meta.module.modules.client

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.managers.HotbarSwitchManager
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.module.modules.player.Sync
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.util.text.NoSpamMessage
import dev.wizard.meta.util.text.format
import net.minecraft.util.text.TextFormatting

object AntiCheat : Module(
    "AntiCheat",
    category = Category.CLIENT,
    description = "Configures bypass for anticheats",
    alwaysListening = true
) {
    var blockPlaceRotation by setting(this, BooleanSetting(settingName("Block Place Rotation"), true))
    val placeRotationBoundingBoxGrow by setting(this, DoubleSetting(settingName("Place Rotation Bounding Box Grow"), 0.1, 0.0..1.0, 0.01))
    val ghostSwitchBypass by setting(this, EnumSetting(settingName("Ghost Switch Bypass"), HotbarSwitchManager.BypassMode.NONE))

    private val swapWarningMessage = "${getChatName()} ${TextFormatting.RED.format("You're using swap mode ghost switch bypass,it is recommended to have Inventory Sync enabled.")}"
    private val warningTimer = TickTimer(TimeUnit.SECONDS)
    private val authTimer = TickTimer(TimeUnit.MINUTES)

    init {
        safeParallelListener<TickEvent.Post> {
            if (ghostSwitchBypass == HotbarSwitchManager.BypassMode.SWAP && Sync.isDisabled() && warningTimer.tickAndReset(5)) {
                val id = (hashCode().toLong() shl 32) or swapWarningMessage.hashCode().toLong()
                NoSpamMessage.sendWarning(id, swapWarningMessage)
            }
        }
    }
}
