package dev.wizard.meta.mixins.core.network

import dev.wizard.meta.module.modules.player.NoRotate
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketConfirmTeleport
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.LocalCapture

@Mixin(NetHandlerPlayClient::class)
class MixinNetHandlerPlayClient {
    @Shadow
    @Final
    private lateinit var field_147302_e: NetworkManager

    @Shadow
    private lateinit var field_147299_f: Minecraft

    @Shadow
    private var field_147309_h = false

    @Inject(method = ["handlePlayerPosLook"], at = [At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setPositionAndRotation(DDDFF)V", shift = At.Shift.BEFORE)], cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    fun handlePlayerPosLook_Inject(packetIn: SPacketPlayerPosLook, ci: CallbackInfo, entityplayer: EntityPlayer, d0: Double, d1: Double, d2: Double, f: Float, f1: Float) {
        if (NoRotate.isEnabled) {
            ci.cancel()
            entityplayer.setPosition(d0, d1, d2)
            this.field_147302_e.sendPacket(CPacketConfirmTeleport(packetIn.teleportId))
            this.field_147302_e.sendPacket(CPacketPlayer.PositionRotation(entityplayer.posX, entityplayer.entityBoundingBox.minY, entityplayer.posZ, f, f1, false))
            if (!this.field_147309_h) {
                this.field_147299_f.player.prevPosX = this.field_147299_f.player.posX
                this.field_147299_f.player.prevPosY = this.field_147299_f.player.posY
                this.field_147299_f.player.prevPosZ = this.field_147299_f.player.posZ
                this.field_147309_h = true
                this.field_147299_f.displayGuiScreen(null)
            }
        }
    }
}
