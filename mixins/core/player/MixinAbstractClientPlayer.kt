package dev.wizard.meta.mixins.core.player

import com.mojang.authlib.GameProfile
import dev.wizard.meta.module.modules.misc.AltProtect
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.GameType
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.Objects

@Mixin(value = [AbstractClientPlayer::class], priority = 0x7FFFFFFF)
abstract class MixinAbstractClientPlayer(worldIn: World, gameProfileIn: GameProfile) : EntityPlayer(worldIn, gameProfileIn) {
    @Inject(method = ["isSpectator"], at = [At("HEAD")], cancellable = true)
    fun isSpectator$Inject$HEAD(cir: CallbackInfoReturnable<Boolean>) {
        val connection = Minecraft.getMinecraft().connection
        if (connection != null) {
            val networkplayerinfo = connection.getPlayerInfo(this.gameProfile.id)
            cir.returnValue = networkplayerinfo != null && networkplayerinfo.gameType == GameType.SPECTATOR
        } else {
            cir.returnValue = false
        }
    }

    @Inject(method = ["hasSkin"], at = [At("HEAD")], cancellable = true)
    private fun doesHaveSkin(cir: CallbackInfoReturnable<Boolean>) {
        val self = this as AbstractClientPlayer
        if (Objects.equals(self.name, AltProtect.currentName) && AltProtect.isEnabled && AltProtect.skinProtect) {
            cir.returnValue = false
        }
    }
}
