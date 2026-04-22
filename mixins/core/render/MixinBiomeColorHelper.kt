package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.Ambiance
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.biome.BiomeColorHelper
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(BiomeColorHelper::class)
abstract class MixinBiomeColorHelper {
    companion object {
        @Inject(method = ["getGrassColorAtPos"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun overrideGrass(world: IBlockAccess, pos: BlockPos, cir: CallbackInfoReturnable<Int>) {
            if (Ambiance.isEnabled) {
                cir.returnValue = Ambiance.grass
            }
        }

        @Inject(method = ["getFoliageColorAtPos"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun overrideFoliage(world: IBlockAccess, pos: BlockPos, cir: CallbackInfoReturnable<Int>) {
            if (Ambiance.isEnabled) {
                cir.returnValue = Ambiance.foliage
            }
        }

        @Inject(method = ["getWaterColorAtPos"], at = [At("HEAD")], cancellable = true)
        @JvmStatic
        private fun overrideWater(world: IBlockAccess, pos: BlockPos, cir: CallbackInfoReturnable<Int>) {
            if (Ambiance.isEnabled) {
                cir.returnValue = Ambiance.water
            }
        }
    }
}
