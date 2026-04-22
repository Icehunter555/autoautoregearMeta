package dev.wizard.meta.mixins.core.network

import dev.wizard.meta.event.events.PacketEvent
import dev.wizard.meta.module.modules.combat.CrystalPlaceBreak
import dev.wizard.meta.module.modules.misc.AntiKick
import io.netty.channel.ChannelHandlerContext
import net.minecraft.client.Minecraft
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.network.play.server.SPacketSpawnObject
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(NetworkManager::class)
class MixinNetworkManager {
    @Inject(method = ["sendPacket(Lnet/minecraft/network/Packet;)V"], at = [At("HEAD")], cancellable = true)
    private fun sendPacketPre(packet: Packet<*>, callbackInfo: CallbackInfo) {
        if (!this.isClient) {
            return
        }
        if (packet != null) {
            val event = PacketEvent.Send(packet)
            event.post()
            if (event.isCancelled) {
                callbackInfo.cancel()
            }
        }
    }

    @Inject(method = ["sendPacket(Lnet/minecraft/network/Packet;)V"], at = [At("RETURN")])
    private fun sendPacketPost(packet: Packet<*>, callbackInfo: CallbackInfo) {
        if (!this.isClient) {
            return
        }
        if (packet != null) {
            val event = PacketEvent.PostSend(packet)
            event.post()
        }
    }

    @Inject(method = ["channelRead0*"], at = [At("HEAD")], cancellable = true)
    private fun channelReadPre(context: ChannelHandlerContext, packet: Packet<*>, callbackInfo: CallbackInfo) {
        if (!this.isClient) {
            return
        }
        if (packet != null) {
            if (packet is SPacketSpawnObject) {
                CrystalPlaceBreak.handleSpawnPacket(packet)
            } else if (packet is SPacketSoundEffect) {
                CrystalPlaceBreak.handleExplosion(packet)
            }
            val event = PacketEvent.Receive(packet)
            event.post()
            if (event.isCancelled) {
                callbackInfo.cancel()
            }
        }
    }

    @Inject(method = ["channelRead0"], at = [At("RETURN")])
    private fun channelReadPost(context: ChannelHandlerContext, packet: Packet<*>, callbackInfo: CallbackInfo) {
        if (!this.isClient) {
            return
        }
        if (packet != null) {
            val event = PacketEvent.PostReceive(packet)
            event.post()
        }
    }

    @Inject(method = ["exceptionCaught"], at = [At("HEAD")], cancellable = true)
    private fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, throwable: Throwable, ci: CallbackInfo) {
        if (!this.isClient) {
            return
        }
        if (AntiKick.isEnabled) {
            AntiKick.sendWarning(throwable)
            ci.cancel()
        }
    }

    private val isClient: Boolean
        get() {
            val casted = this as NetworkManager
            val connection = Minecraft.getMinecraft().connection
            return connection != null && casted === connection.networkManager
        }
}
