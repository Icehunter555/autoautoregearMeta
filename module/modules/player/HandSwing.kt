package dev.wizard.meta.module.modules.player

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.event.events.RunGameLoopEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.network.play.client.CPacketAnimation

object HandSwing : Module(
    "Hand Swing",
    category = Category.PLAYER,
    description = "Modifies hand swing animation"
) {
    private val cancelClientSide by setting(this, BooleanSetting(settingName("Cancel Client Side"), false))
    private val cancelServerSide by setting(this, BooleanSetting(settingName("Cancel Server Side"), false))
    val swingTicks by setting(this, IntegerSetting(settingName("Swing ticks"), -1, -1..20, 1, { !cancelClientSide }))
    val cancelEquipAnimation by setting(this, BooleanSetting(settingName("Cancel Equip Animation"), false))

    val modifiedSwingSpeed: Boolean
        get() = isEnabled && !cancelClientSide && swingTicks != -1

    init {
        listener<PacketEvent.Send> {
            if (cancelServerSide && it.packet is CPacketAnimation) {
                it.cancel()
            }
        }

        safeListener<RunGameLoopEvent.Render> {
            if (cancelClientSide) {
                player.isSwingInProgress = false
                player.swingProgressInt = 0
                player.swingProgress = 0.0f
                player.prevSwingProgress = 0.0f
            }
        }
    }
}
