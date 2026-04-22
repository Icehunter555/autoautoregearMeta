package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.combat.BounceBegone
import net.minecraft.block.BlockBed
import net.minecraft.entity.Entity
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(BlockBed::class)
class MixinBlockBed {
    @Inject(method = ["onLanded"], at = [At("HEAD")], cancellable = true)
    private fun cancelBedBounce(worldIn: World, entityIn: Entity, ci: CallbackInfo) {
        if (BounceBegone.stopBounce()) {
            ci.cancel()
        }
    }
}
