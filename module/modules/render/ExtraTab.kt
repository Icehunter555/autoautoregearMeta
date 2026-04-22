package dev.wizard.meta.module.modules.render

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.network.play.server.SPacketPlayerListHeaderFooter

object ExtraTab : Module(
    "ExtraTab",
    category = Category.RENDER,
    description = "Expands the player tab menu"
) {
    private val tabSize by setting(this, IntegerSetting(settingName("Max Players"), 265, 80..400, 5))
    private val vanillaTab by setting(this, BooleanSetting(settingName("Vanilla Tab"), false))

    override fun getHudInfo(): String = tabSize.toString()

    @JvmStatic
    fun subList(list: List<NetworkPlayerInfo>, newList: List<NetworkPlayerInfo>): List<NetworkPlayerInfo> {
        return if (isDisabled) newList else list.subList(0, list.size.coerceAtMost(tabSize))
    }

    init {
        safeListener<PacketEvent.Receive> {
            if (it.packet is SPacketPlayerListHeaderFooter && vanillaTab) {
                it.cancel()
            }
        }
    }
}
