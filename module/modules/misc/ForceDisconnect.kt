package dev.wizard.meta.module.modules.misc

import dev.wizard.meta.module.Category
import dev.wizard.meta.module.Module
import net.minecraft.network.play.client.CPacketPlayer
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object ForceDisconnect : Module(
    name = "ForceDisconnect",
    category = Category.MISC,
    description = "kick you from the server",
    visible = true
) {
    @JvmStatic
    fun handleButtonPress(ci: CallbackInfo) {
        if (mc.isSingleplayer || mc.connection == null) return

        val connection = mc.connection
        if (connection != null) {
            connection.sendPacket(CPacketPlayer.Position(Double.POSITIVE_INFINITY, 255.0, Double.POSITIVE_INFINITY, true))
            
            if (AutoReconnect.isEnabled) {
                AutoReconnect.hasKicked = true
            }
            ci.cancel()
        }
    }
}
