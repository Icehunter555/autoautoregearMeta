package dev.wizard.meta.mixins.core.gui

import dev.wizard.meta.module.modules.misc.ChatTweaks
import dev.wizard.meta.module.modules.render.ExtraTab
import net.minecraft.client.gui.GuiPlayerTabOverlay
import net.minecraft.client.network.NetworkPlayerInfo
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(GuiPlayerTabOverlay::class)
class MixinGuiPlayerTabOverlay {
    private var preSubList: List<NetworkPlayerInfo> = emptyList()

    @ModifyVariable(method = ["renderPlayerlist"], at = At(value = "STORE", ordinal = 0), ordinal = 0)
    fun renderPlayerlistStorePlayerListPre(list2: List<NetworkPlayerInfo>): List<NetworkPlayerInfo> {
        this.preSubList = list2
        return list2
    }

    @ModifyVariable(method = ["renderPlayerlist"], at = At(value = "STORE", ordinal = 1), ordinal = 0)
    fun renderPlayerlistStorePlayerListPost(list2: List<NetworkPlayerInfo>): List<NetworkPlayerInfo> {
        return ExtraTab.subList(this.preSubList, list2)
    }

    @Inject(method = ["getPlayerName"], at = [At("HEAD")], cancellable = true)
    fun getPlayerName(networkPlayerInfoIn: NetworkPlayerInfo, cir: CallbackInfoReturnable<String>) {
        ChatTweaks.getPlayerName(networkPlayerInfoIn, cir)
    }
}
