package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.module.modules.player.Freecam
import dev.wizard.meta.module.modules.render.Ambiance
import dev.wizard.meta.module.modules.render.ESP
import dev.wizard.meta.module.modules.render.HighLight
import dev.wizard.meta.module.modules.render.NoRender
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.DestroyBlockProgress
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.shader.ShaderGroup
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [RenderGlobal::class], priority = 0x7FFFFFFF)
abstract class MixinRenderGlobal {
    @Shadow
    @Final
    private lateinit var field_72738_E: Map<Int, DestroyBlockProgress>

    @Shadow
    private var field_72769_h: WorldClient? = null

    @Shadow
    @Final
    private lateinit var field_72777_q: Minecraft

    @Shadow
    private var field_174991_A: ShaderGroup? = null

    private var prevShaderGroup: ShaderGroup? = null

    @Shadow
    protected abstract fun func_180443_s()

    @Shadow
    protected abstract fun func_174969_t()

    @Shadow
    protected abstract fun func_180448_r()

    @Inject(method = ["drawSelectionBox"], at = [At("HEAD")], cancellable = true)
    fun drawSelectionBox(player: EntityPlayer, movingObjectPositionIn: RayTraceResult, execute: Int, partialTicks2: Float, ci: CallbackInfo) {
        if (HighLight.isEnabled) {
            ci.cancel()
        }
    }

    @Inject(method = ["renderEntities"], at = [At("HEAD")])
    fun renderEntitiesHead(renderViewEntity: Entity, camera: ICamera, partialTicks2: Float, ci: CallbackInfo) {
        RenderEntityEvent.renderingEntities = true
    }

    @Inject(method = ["renderEntities"], at = [At("RETURN")])
    fun renderEntitiesReturn(renderViewEntity: Entity, camera: ICamera, partialTicks2: Float, ci: CallbackInfo) {
        RenderEntityEvent.renderingEntities = false
    }

    @Inject(method = ["renderClouds"], at = [At("HEAD")], cancellable = true)
    fun cancelClouds(ci: CallbackInfo) {
        if (NoRender.isEnabled && NoRender.noClouds) {
            ci.cancel()
        }
    }

    @ModifyVariable(method = ["setupTerrain"], at = At(value = "STORE", ordinal = 0), ordinal = 1)
    fun setupTerrainStoreFlooredChunkPosition(playerPos: BlockPos): BlockPos {
        var pos = playerPos
        if (Freecam.isEnabled) {
            pos = Freecam.getRenderChunkOffset(pos)
        }
        return pos
    }

    @Inject(method = ["renderSky(FI)V"], at = [At("HEAD")], cancellable = true)
    private fun renderEndSky(partialTicks2: Float, pass: Int, ci: CallbackInfo) {
        if (Ambiance.endSky) {
            this.func_180448_r()
            ci.cancel()
        }
    }

    @Inject(method = ["renderEntities"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V", ordinal = 0, shift = At.Shift.BEFORE)])
    private fun Inject$renderEntities$INVOKE$ShaderGroup$render$BEFORE(renderViewEntity: Entity, camera: ICamera, partialTicks2: Float, ci: CallbackInfo) {
        if (ESP.outlineESP) {
            this.prevShaderGroup = this.field_174991_A
            this.field_174991_A = ESP.NoOpShaderGroup
        }
    }

    @Inject(method = ["renderEntities"], at = [At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableColorMaterial()V", shift = At.Shift.AFTER)])
    private fun Inject$renderEntities$INVOKE$ShaderGroup$render$AFTER(renderViewEntity: Entity, camera: ICamera, partialTicks2: Float, ci: CallbackInfo) {
        val prev = this.prevShaderGroup
        this.prevShaderGroup = null
        if (prev != null) {
            this.field_174991_A = prev
        }
    }

    @Inject(method = ["renderEntityOutlineFramebuffer"], at = [At("HEAD")], cancellable = true)
    private fun Inject$renderEntityOutlineFramebuffer(ci: CallbackInfo) {
        if (ESP.outlineESP) {
            ci.cancel()
        }
    }
}
