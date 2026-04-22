package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.DyeSpoofer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BlockRendererDispatcher
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(BlockRendererDispatcher::class)
class MixinBlockRenderer {
    @ModifyVariable(method = ["renderBlock"], at = At("HEAD"), argsOnly = true)
    private fun modifyBlockState(value: IBlockState): IBlockState {
        if (DyeSpoofer.isEnabled) {
            return DyeSpoofer.handleBlockState(value)
        }
        return value
    }
}
