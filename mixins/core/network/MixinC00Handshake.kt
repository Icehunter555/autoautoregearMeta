package dev.wizard.meta.mixins.core.network

import dev.wizard.meta.module.modules.misc.FakeVanillaClient
import net.minecraft.network.EnumConnectionState
import net.minecraft.network.PacketBuffer
import net.minecraft.network.handshake.client.C00Handshake
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(C00Handshake::class)
class MixinC00Handshake {
    @Shadow
    private var field_149600_a = 0

    @Shadow
    private lateinit var field_149598_b: String

    @Shadow
    private var field_149599_c = 0

    @Shadow
    private lateinit var field_149597_d: EnumConnectionState

    @Inject(method = ["writePacketData"], at = [At("HEAD")], cancellable = true)
    fun writePacketData(buf: PacketBuffer, info: CallbackInfo) {
        if (FakeVanillaClient.isEnabled) {
            info.cancel()
            buf.writeVarInt(this.field_149600_a)
            buf.writeString(this.field_149598_b)
            buf.writeShort(this.field_149599_c)
            buf.writeVarInt(this.field_149597_d.id)
        }
    }
}
