package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.module.modules.render.ESP
import dev.wizard.meta.module.modules.render.Nametags
import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Render::class)
abstract class MixinRender<T : Entity> {
    @Inject(method = ["renderName"], at = [At("HEAD")], cancellable = true)
    protected fun renderName$Inject$HEAD(entity: T, x: Double, y: Double, z: Double, ci: CallbackInfo) {
        if (Nametags.isEnabled && Nametags.checkEntityType(entity)) {
            ci.cancel()
        }
    }

    @Inject(method = ["getTeamColor"], at = [At("HEAD")], cancellable = true)
    fun getTeamColor$Inject$HEAD(entityIn: T, cir: CallbackInfoReturnable<Int>) {
        val color = ESP.getEspColor(entityIn)
        if (color != null) {
            cir.returnValue = color
        }
    }

    @Inject(method = ["renderEntityOnFire"], at = [At("HEAD")], cancellable = true)
    fun cancelFireRendering(entity: Entity, x: Double, y: Double, z: Double, partialTicks2: Float, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.noEntityFire) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderShadow"], at = [At("HEAD")], cancellable = true)
    fun cancelShadows(entityIn: Entity, x: Double, y: Double, z: Double, shadowAlpha: Float, partialTicks2: Float, ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.noEntityShadow) {
            ci.cancel()
        }
    }
}
