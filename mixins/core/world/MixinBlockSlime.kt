package dev.wizard.meta.mixins.core.world

import dev.wizard.meta.module.modules.movement.Velocity
import net.minecraft.block.BlockSlime
import net.minecraft.entity.Entity
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(BlockSlime::class)
class MixinBlockSlime {
    @Inject(method = ["onLanded"], at = [At("HEAD")], cancellable = true)
    private fun cancelSlimeBounce(worldIn: World, entityIn: Entity, ci: CallbackInfo) {
        if (Velocity.shouldCancelSlime()) {
            ci.cancel()
        }
    }
}
