package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.renderer.ViewFrustum
import net.minecraft.client.renderer.chunk.RenderChunk
import net.minecraft.util.math.MathHelper
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ViewFrustum::class)
abstract class MixinViewFrustum {
    @Shadow
    lateinit var field_178164_f: Array<RenderChunk>

    @Shadow
    protected var field_178165_d = 0

    @Shadow
    protected var field_178168_c = 0

    @Shadow
    protected var field_178166_e = 0

    @Shadow
    protected abstract fun func_178157_a(var1: Int, var2: Int, var3: Int): Int

    @Inject(method = ["updateChunkPositions"], at = [At("HEAD")], cancellable = true)
    fun updateChunkPositionsHead(viewEntityX: Double, viewEntityZ: Double, ci: CallbackInfo) {
        if (Freecam.isDisabled) {
            return
        }
        val player = Wrapper.getPlayer() ?: return
        val centerX = MathHelper.floor(player.posX) - 8
        val centerZ = MathHelper.floor(player.posZ) - 8
        val multipliedCountX = this.field_178165_d * 16
        for (x in 0 until this.field_178165_d) {
            val posX = this.func_178157_a(centerX, multipliedCountX, x)
            for (z in 0 until this.field_178166_e) {
                val poxZ = this.func_178157_a(centerZ, multipliedCountX, z)
                for (y in 0 until this.field_178168_c) {
                    val poxY = y * 16
                    val renderchunk = this.field_178164_f[(z * this.field_178168_c + y) * this.field_178165_d + x]
                    renderchunk.setPosition(posX, poxY, poxZ)
                }
            }
        }
        ci.cancel()
    }
}
