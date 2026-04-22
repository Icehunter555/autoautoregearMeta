package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.graphics.RenderUtils3D
import dev.wizard.meta.module.modules.exploit.PacketFlyOld
import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.util.Wrapper
import dev.wizard.meta.util.math.vector.toBlockPos
import net.minecraft.client.renderer.chunk.VisGraph
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.EnumSet

@Mixin(VisGraph::class)
class MixinVisGraph {
    @Inject(method = ["getVisibleFacings"], at = [At("HEAD")], cancellable = true)
    fun getVisibleFacings(ci: CallbackInfoReturnable<Set<EnumFacing>>) {
        if (PacketFlyOld.isDisabled && Freecam.isDisabled) {
            return
        }
        val world = Wrapper.getWorld() ?: return
        val camPos = RenderUtils3D.getCamPos()
        val blockPos = camPos.toBlockPos()
        if (world.getBlockState(blockPos).isFullCube) {
            ci.returnValue = EnumSet.allOf(EnumFacing::class.java)
        }
    }

    @Inject(method = ["setOpaqueCube"], at = [At("HEAD")], cancellable = true)
    fun setOpaqueCube(pos: BlockPos, ci: CallbackInfo) {
    }
}
