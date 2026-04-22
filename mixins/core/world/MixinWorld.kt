package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.render.Ambiance
import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.util.math.BlockPos
import net.minecraft.world.EnumSkyBlock
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(value = [World::class], priority = 0x7FFFFFFF)
abstract class MixinWorld {
    @Inject(method = ["checkLightFor"], at = [At("HEAD")], cancellable = true)
    private fun checkLightForHead(lightType: EnumSkyBlock, pos: BlockPos, ci: CallbackInfoReturnable<Boolean>) {
        if (NoRender.handleLighting(lightType)) {
            ci.returnValue = false
        }
    }

    @Inject(method = ["getThunderStrength"], at = [At("HEAD")], cancellable = true)
    private fun getThunderStrengthHead(delta: Float, cir: CallbackInfoReturnable<Float>) {
        if (Ambiance.shouldCancelWeather()) {
            cir.returnValue = 0.0f
        }
    }

    @Inject(method = ["getRainStrength"], at = [At("HEAD")], cancellable = true)
    private fun getRainStrengthHead(delta: Float, cir: CallbackInfoReturnable<Float>) {
        if (Ambiance.shouldCancelWeather()) {
            cir.returnValue = 0.0f
        }
    }
}
