package dev.wizard.meta.mixins.core.render

import dev.wizard.meta.mixins.accessor.render.AccessorRender
import dev.wizard.meta.module.modules.render.ItemPhysics
import dev.wizard.meta.util.Wrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.entity.RenderEntityItem
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*

@Mixin(RenderEntityItem::class)
abstract class MixinRenderEntityItem {
    @Unique
    private var rotation = 0.0

    @Final
    @Shadow
    private lateinit var field_177079_e: Random

    @Inject(method = ["transformModelCount"], at = [At("HEAD")], cancellable = true)
    private fun onItemAnim(itemIn: EntityItem, p_177077_2_: Double, p_177077_4_: Double, p_177077_6_: Double, p_177077_8_: Float, p_177077_9_: IBakedModel, cir: CallbackInfoReturnable<Int>) {
        if (ItemPhysics.isEnabled) {
            val tick = ItemPhysics.tick
            val rotate = ItemPhysics.rotateSpeed
            val oldRotation = ItemPhysics.oldRotations
            val renderer = this as RenderEntityItem
            this.rotation = (System.nanoTime().toFloat() - tick).toDouble() / 1.0E7 * rotate.toDouble() / 12.5
            if (!mc.isGamePaused) {
                this.rotation = 0.0
            }
            val itemstack = itemIn.item
            this.field_177079_e.setSeed(if (itemstack != null && itemstack.item != null) (Item.getIdFromItem(itemstack.item) + itemstack.metadata).toLong() else 187L)
            GlStateManager.pushMatrix()
            renderer.bindTexture(this.`trollHack$getEntityTexture`())
            renderer.renderManager.renderEngine.getTexture(this.`trollHack$getEntityTexture`()).setBlurMipmap(false, false)
            GlStateManager.enableRescaleNormal()
            GlStateManager.alphaFunc(516, 0.1f)
            GlStateManager.enableBlend()
            RenderHelper.enableStandardItemLighting()
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO)
            GlStateManager.pushMatrix()
            val ibakedmodel = mc.renderItem.getItemModelWithOverrides(itemstack, itemIn.world, null)
            val is3D = ibakedmodel.isGui3d
            var count = 1
            if (itemstack.stackSize > 48) {
                count = 5
            } else if (itemstack.stackSize > 32) {
                count = 4
            } else if (itemstack.stackSize > 16) {
                count = 3
            } else if (itemstack.stackSize > 1) {
                count = 2
            }
            GlStateManager.translate(p_177077_2_.toFloat(), p_177077_4_.toFloat(), p_177077_6_.toFloat())
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f)
            GL11.glRotatef(itemIn.rotationYaw, 0.0f, 0.0f, 1.0f)
            if (is3D) {
                GlStateManager.translate(0.0, -0.2, -0.08)
            } else {
                GlStateManager.translate(0.0, 0.0, -0.04)
            }
            if (is3D || mc.renderManager.options != null) {
                if (is3D) {
                    if (!itemIn.onGround) {
                        itemIn.rotationPitch = itemIn.rotationPitch + this.rotation.toFloat()
                    } else if (oldRotation && itemIn.rotationPitch != 0.0f && itemIn.rotationPitch != 90.0f && itemIn.rotationPitch != 180.0f && itemIn.rotationPitch != 270.0f) {
                        val minIndex = `trollHack$getIndex`(itemIn)
                        when (minIndex) {
                            0 -> itemIn.rotationPitch += (if (itemIn.rotationPitch < 0.0f) this.rotation else -this.rotation).toFloat()
                            1 -> itemIn.rotationPitch += (if (itemIn.rotationPitch - 90.0f < 0.0f) this.rotation else -this.rotation).toFloat()
                            2 -> itemIn.rotationPitch += (if (itemIn.rotationPitch - 180.0f < 0.0f) this.rotation else -this.rotation).toFloat()
                            3 -> itemIn.rotationPitch += (if (itemIn.rotationPitch - 270.0f < 0.0f) this.rotation else -this.rotation).toFloat()
                        }
                    }
                } else if (!(java.lang.Double.isNaN(itemIn.posX) || java.lang.Double.isNaN(itemIn.posY) || java.lang.Double.isNaN(itemIn.posZ) || itemIn.world == null)) {
                    itemIn.rotationPitch = if (itemIn.onGround) 180.0f else (itemIn.rotationPitch.toDouble() + this.rotation).toFloat()
                }
                val height = 0.2
                if (is3D) {
                    GlStateManager.translate(0.0, height, 0.0)
                }
                GlStateManager.rotate(itemIn.rotationPitch, 1.0f, 0.0f, 0.0f)
                if (is3D) {
                    GlStateManager.translate(0.0, -height, 0.0)
                }
            }
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            val scale = ItemPhysics.scale
            GlStateManager.scale(scale, scale, scale)
            val renderOutlines = (this as AccessorRender).renderOutlines
            val xScale = ibakedmodel.itemCameraTransforms.ground.scale.x
            val yScale = ibakedmodel.itemCameraTransforms.ground.scale.y
            val zScale = ibakedmodel.itemCameraTransforms.ground.scale.z
            if (!is3D) {
                val xTranslation = -0.0f * (count - 1).toFloat() * xScale
                val yTranslation = -0.0f * (count - 1).toFloat() * yScale
                val zTranslation = -0.09375f * (count - 1).toFloat() * 0.5f * zScale
                GlStateManager.translate(xTranslation, yTranslation, zTranslation)
            }
            if (renderOutlines) {
                GlStateManager.enableColorMaterial()
                try {
                    GlStateManager.enableOutlineMode((this as AccessorRender).callGetTeamColor(itemIn))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            for (k in 0 until count) {
                GlStateManager.pushMatrix()
                if (is3D) {
                    if (k > 0) {
                        val xTranslation = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f + 0.2f
                        val yTranslation = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f
                        val zTranslation = (this.field_177079_e.nextFloat() * 2.0f - 1.0f) * 0.15f + 0.2f
                        GlStateManager.translate(xTranslation, yTranslation, zTranslation)
                    }
                    mc.renderItem.renderItem(itemstack, ibakedmodel)
                    GlStateManager.popMatrix()
                    continue
                }
                mc.renderItem.renderItem(itemstack, ibakedmodel)
                GlStateManager.popMatrix()
                GlStateManager.translate(0.0, 0.0, 0.09375)
            }
            if (renderOutlines) {
                GlStateManager.disableOutlineMode()
                GlStateManager.disableColorMaterial()
            }
            GlStateManager.popMatrix()
            GlStateManager.disableRescaleNormal()
            GlStateManager.disableBlend()
            renderer.bindTexture(this.`trollHack$getEntityTexture`())
            renderer.renderManager.renderEngine.getTexture(this.`trollHack$getEntityTexture`()).restoreLastBlurMipmap()
            GlStateManager.popMatrix()
            cir.cancel()
        }
    }

    @Unique
    private fun `trollHack$getEntityTexture`(): ResourceLocation {
        return TextureMap.LOCATION_BLOCKS_TEXTURE
    }

    companion object {
        @Unique
        private val mc = Wrapper.getMinecraft()

        @Unique
        private fun `trollHack$getIndex`(itemIn: EntityItem): Int {
            val dirs = doubleArrayOf(Math.abs(itemIn.rotationPitch.toDouble()), Math.abs(itemIn.rotationPitch - 90.0f).toDouble(), Math.abs(itemIn.rotationPitch - 180.0f).toDouble(), Math.abs(itemIn.rotationPitch - 270.0f).toDouble())
            var minDir = dirs[0]
            var minIndex = 0
            for (i in 1 until dirs.size) {
                if (dirs[i] < minDir) {
                    minDir = dirs[i]
                    minIndex = i
                }
            }
            return minIndex
        }
    }
}
