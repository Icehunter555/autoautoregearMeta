package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.event.events.render.RenderEntityEvent
import dev.wizard.meta.graphics.GlStateUtils
import dev.wizard.meta.module.modules.render.CrystalChams
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelBase
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderDragon
import net.minecraft.client.renderer.entity.RenderEnderCrystal
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL11
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(RenderEnderCrystal::class)
abstract class MixinRenderEnderCrystal(renderManager: RenderManager) : Render<EntityEnderCrystal>(renderManager) {
    @Shadow
    @Final
    private lateinit var field_76995_b: ModelBase

    @Shadow
    @Final
    private lateinit var field_188316_g: ModelBase

    @Inject(method = ["doRender"], at = [At("HEAD")], cancellable = true)
    fun doRender$Inject$HEAD(entity: EntityEnderCrystal, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks2: Float, ci: CallbackInfo) {
        if (!CrystalChams.isEnabled || RenderEntityEvent.renderingEntities) {
            return
        }
        val spinTicks = entity.innerRotation + partialTicks2
        var floatTicks = MathHelper.sin(spinTicks * 0.2f * CrystalChams.floatSpeed) / 2.0f + 0.5f
        val scale = CrystalChams.scale
        val spinSpeed = CrystalChams.spinSpeed
        floatTicks = floatTicks * floatTicks + floatTicks
        val model = if (entity.shouldShowBottom()) this.field_76995_b else this.field_188316_g
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, z)
        GlStateManager.scale(scale, scale, scale)
        if (CrystalChams.filled) {
            CrystalChams.setFilledColor()
            GlStateUtils.depth(CrystalChams.filledDepth)
            model.render(entity, 0.0f, spinTicks * 3.0f * spinSpeed, floatTicks * 0.2f, 0.0f, 0.0f, 0.0625f)
        }
        if (CrystalChams.outline) {
            CrystalChams.setOutlineColor()
            GlStateUtils.depth(CrystalChams.outlineDepth)
            GlStateManager.glPolygonMode(1032, 6913)
            model.render(entity, 0.0f, spinTicks * 3.0f * spinSpeed, floatTicks * 0.2f, 0.0f, 0.0f, 0.0625f)
            GlStateManager.glPolygonMode(1032, 6914)
        }
        if (CrystalChams.model == CrystalChams.Model.XQZ) {
            GL11.glEnable(32823)
            GlStateManager.enablePolygonOffset()
            GL11.glPolygonOffset(1.0f, -1000000.0f)
            CrystalChams.setModelColor()
            model.render(entity, 0.0f, spinTicks * 3.0f * spinSpeed, floatTicks * 0.2f, 0.0f, 0.0f, 0.0625f)
            GL11.glDisable(32823)
            GlStateManager.disablePolygonOffset()
            GL11.glPolygonOffset(1.0f, 1000000.0f)
        }
        if (CrystalChams.glint) {
            GlStateManager.enableBlend()
            GlStateUtils.alpha(true)
            GlStateUtils.depth(CrystalChams.glintDepth)
            Minecraft.getMinecraft().textureManager.bindTexture(ResourceLocation("textures/misc/enchanted_item_glint.png"))
            CrystalChams.setGlintColor()
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
            GlStateManager.matrixMode(5890)
            GlStateManager.pushMatrix()
            for (i in 0..1) {
                GlStateManager.loadIdentity()
                GlStateManager.rotate(30.0f - i.toFloat() * 60.0f, 0.0f, 0.0f, 1.0f)
                GlStateManager.translate(0.0f, (entity.ticksExisted.toFloat() + partialTicks2) * (0.001f + i.toFloat() * 0.003f) * 20.0f, 0.0f)
                GlStateManager.matrixMode(5888)
                model.render(entity, 0.0f, spinTicks * 3.0f * spinSpeed, floatTicks * 0.2f, 0.0f, 0.0f, 0.0625f)
                GlStateManager.matrixMode(5890)
            }
            GlStateManager.popMatrix()
            GlStateManager.matrixMode(5888)
            GlStateManager.depthMask(true)
        }
        GlStateManager.popMatrix()
        val blockpos = entity.beamTarget
        if (blockpos != null) {
            this.bindTexture(RenderDragon.ENDERCRYSTAL_BEAM_TEXTURES)
            val posX = (blockpos.x.toFloat() + 0.5f).toDouble() - entity.posX
            val posY = (blockpos.y.toFloat() + 0.5f).toDouble() - entity.posY
            val posZ = (blockpos.z.toFloat() + 0.5f).toDouble() - entity.posZ
            RenderDragon.renderCrystalBeams(x + posX, y - 0.3 + (floatTicks * 0.4f).toDouble() + posY, z + posZ, partialTicks2, (blockpos.x.toFloat() + 0.5f).toDouble(), (blockpos.y.toFloat() + 0.5f).toDouble(), (blockpos.z.toFloat() + 0.5f).toDouble(), entity.innerRotation, entity.posX, entity.posY, entity.posZ)
        }
        GlStateManager.disableTexture2D()
        ci.cancel()
    }
}
